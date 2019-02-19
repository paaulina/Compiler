import java.util.Hashtable;
import java.util.Map;
import java.util.stream.Stream;

public class CodeGenerator implements CommandType {
    private Map<String, AboutSymbol> symbols = new Hashtable<>();
    private long freeAddress = 0;
    public char reg = 'A';
    public boolean mutable = false;
    public String result = "";
    public Label jump = null;
    private AboutSymbol toBeInitialised = null;

    public static Stream<Command> concat(Stream<Command>... streams) {
        Stream<Command> result = Stream.empty();
        for (Stream<Command> stream : streams) {
            result = Stream.concat(result, stream);
        }
        return result;
    }

    public <T> T error(String description) {
        throw new CompilationError(description);
    }

    public void program(Stream<Command> cs) {
        StringBuilder builder = new StringBuilder();
        Command[] commands = cs.toArray(Command[]::new);
        long programCounter = 0;
        for (Command command : commands) {
            if (!command.solveLabel(programCounter)) {
                programCounter++;
            }
        }
        for (Command command : commands) {
            builder.append(command);
        }
        builder.append("HALT\n");
        result = builder.toString();
    }

    public void newScalar(String id, boolean isIter) {
        if (symbols.containsKey(id)) {
            error("Redeklaracja zmiennej");
        }
        AboutSymbol about = new AboutSymbol();
        about.isArray = false;
        about.isIter = isIter;
        about.address = freeAddress++;
        if (about.isIter) {
            freeAddress++;
        }
        about.initialised = isIter;
        symbols.put(id, about);
    }

    public void newArray(String id, long first, long last) {
        if (symbols.containsKey(id)) {
            error("Redeklaracja zmiennej");
        }
        if (last < first) {
            error("Pusty zakres tablicy");
        }
        AboutSymbol about = new AboutSymbol();
        about.isArray = true;
        about.address = freeAddress;
        about.firstIndex = first;
        about.lastIndex = last;
        about.initialised = false;
        freeAddress += last - first + 1;
        symbols.put(id, about);
    }

    public Stream<Command> ifThenElse(Stream<Command> cond, Stream<Command> ifTrue, Stream<Command> ifFalse, Label fail) {
        Label end = new Label();
        return concat(
                cond,
                ifTrue,
                new Command(JUMP, end).emit(),
                new Command(LABEL, fail).emit(),
                ifFalse,
                new Command(LABEL, end).emit()
        );
    }

    public Stream<Command> ifThen(Stream<Command> cond, Stream<Command> ifTrue, Label end) {
        return concat(
                cond,
                ifTrue,
                new Command(LABEL, end).emit()
        );
    }

    public Stream<Command> whileLoop(Stream<Command> cond, Stream<Command> body, Label end) {
        Label start = new Label();
        return concat(
                new Command(LABEL, start).emit(),
                cond,
                body,
                new Command(JUMP, start).emit(),
                new Command(LABEL, end).emit()
        );
    }

    public Stream<Command> doLoop(Stream<Command> body, Stream<Command> cond, Label end) {
        Label start = new Label();
        return concat(
                new Command(LABEL, start).emit(),
                body,
                cond,
                new Command(JUMP, start).emit(),
                new Command(LABEL, end).emit()
        );
    }

    public Stream<Command> forLoop(String it, Stream<Command> from, Stream<Command> to, Stream<Command> body, boolean down) {
        freeAddress -= 2;
        symbols.remove(it);
        Stream<Command> span;
        if (down) {
            span = concat(
                    new Command(COPY, 'D', 'B').emit(),
                    new Command(INC, 'D').emit(),
                    new Command(SUB, 'D', 'C').emit()
            );
        } else {
            span = concat(
                    new Command(COPY, 'D', 'C').emit(),
                    new Command(INC, 'D').emit(),
                    new Command(SUB, 'D', 'B').emit()
            );
        }
        Label test = new Label();
        Label end = new Label();
        return concat(
                from,
                to,
                span,
                number('A', freeAddress),
                new Command(LABEL, test).emit(),
                new Command(JZERO, 'D', end).emit(),
                new Command(STORE, 'B').emit(),
                new Command(INC, 'A').emit(),
                new Command(STORE, 'D').emit(),
                body,
                number('A', freeAddress + 1),
                new Command(LOAD, 'D').emit(),
                new Command(DEC, 'D').emit(),
                new Command(DEC, 'A').emit(),
                new Command(LOAD, 'B').emit(),
                new Command(down ? DEC : INC, 'B').emit(),
                new Command(JUMP, test).emit(),
                new Command(LABEL, end).emit()
        );
    }

    public Stream<Command> assign(Stream<Command> id, Stream<Command> expr) {
        initialise();
        return concat(expr, id, new Command(STORE, reg).emit());
    }

    public Stream<Command> read(Stream<Command> id) {
        initialise();
        return concat(id, new Command(GET, 'B').emit(), new Command(STORE, 'B').emit());
    }

    private void initialise() {
        if (toBeInitialised != null) {
            toBeInitialised.initialised = true;
            toBeInitialised = null;
        }
    }

    public Stream<Command> write(Stream<Command> val) {
        return concat(val, new Command(PUT, 'B').emit());
    }

    public Stream<Command> plus(Stream<Command> v1, Stream<Command> v2) {
        return concat(v1, v2, new Command(ADD, 'B', 'C').emit());
    }

    public Stream<Command> minus(Stream<Command> v1, Stream<Command> v2) {
        return concat(v1, v2, new Command(SUB, 'B', 'C').emit());
    }

    public Stream<Command> times(Stream<Command> v1, Stream<Command> v2) {
        Label test = new Label();
        Label odd = new Label();
        Label after_odd = new Label();
        Label end = new Label();
        return concat(v1, v2,
                new Command(SUB, 'D', 'D').emit(),
                new Command(LABEL, test).emit(),
                new Command(JZERO, 'B', end).emit(),
                new Command(JODD, 'B', odd).emit(),
                new Command(JUMP, after_odd).emit(),
                new Command(LABEL, odd).emit(),
                new Command(ADD, 'D', 'C').emit(),
                new Command(LABEL, after_odd).emit(),
                new Command(HALF, 'B').emit(),
                new Command(ADD, 'C', 'C').emit(),
                new Command(JUMP, test).emit(),
                new Command(LABEL, end).emit());
    }

    public Stream<Command> divide(Stream<Command> v1, Stream<Command> v2) {
        Label end = new Label();
        Label loop1 = new Label();
        Label loop1end = new Label();
        Label loop2test = new Label();
        Label loop2body = new Label();
        Label loop3 = new Label();
        return concat(v1, v2,
                new Command(SUB, 'D', 'D').emit(),
                new Command(JZERO, 'C', end).emit(),
                new Command(LABEL, loop1).emit(),
                new Command(JODD, 'C', loop1end).emit(),
                new Command(HALF, 'C').emit(),
                new Command(HALF, 'B').emit(),
                new Command(JUMP, loop1).emit(),
                new Command(LABEL, loop1end).emit(),
                new Command(JUMP, loop2test).emit(),
                new Command(LABEL, loop2body).emit(),
                new Command(ADD, 'C', 'C').emit(),
                new Command(LABEL, loop2test).emit(),
                new Command(COPY, 'E', 'C').emit(),
                new Command(SUB, 'E', 'B').emit(),
                new Command(JZERO, 'E', loop2body).emit(),
                new Command(LABEL, loop3).emit(),
                new Command(JODD, 'C', end).emit(),
                new Command(ADD, 'D', 'D').emit(),
                new Command(HALF, 'C').emit(),
                new Command(SUB, 'F', 'F').emit(),
                new Command(INC, 'F').emit(),
                new Command(COPY, 'E', 'C').emit(),
                new Command(SUB, 'E', 'B').emit(),
                new Command(SUB, 'F', 'E').emit(),
                new Command(JZERO, 'F', loop3).emit(),
                new Command(SUB, 'B', 'C').emit(),
                new Command(INC, 'D').emit(),
                new Command(JUMP, loop3).emit(),
                new Command(LABEL, end).emit()
        );
    }

    public Stream<Command> modulo(Stream<Command> v1, Stream<Command> v2) {
        Label ifzero = new Label();
        Label loop1test = new Label();
        Label loop1body = new Label();
        Label loop2 = new Label();
        Label end = new Label();
        return concat(v1, v2,
                new Command(JZERO, 'C', ifzero).emit(),
                new Command(SUB, 'D', 'D').emit(),
                new Command(JUMP, loop1test).emit(),
                new Command(LABEL, loop1body).emit(),
                new Command(ADD, 'C', 'C').emit(),
                new Command(INC, 'D').emit(),
                new Command(LABEL, loop1test).emit(),
                new Command(COPY, 'E', 'C').emit(),
                new Command(SUB, 'E', 'B').emit(),
                new Command(JZERO, 'E', loop1body).emit(),
                new Command(LABEL, loop2).emit(),
                new Command(JZERO, 'D', end).emit(),
                new Command(HALF, 'C').emit(),
                new Command(DEC, 'D').emit(),
                new Command(SUB, 'F', 'F').emit(),
                new Command(INC, 'F').emit(),
                new Command(COPY, 'E', 'C').emit(),
                new Command(SUB, 'E', 'B').emit(),
                new Command(SUB, 'F', 'E').emit(),
                new Command(JZERO, 'F', loop2).emit(),
                new Command(SUB, 'B', 'C').emit(),
                new Command(JUMP, loop2).emit(),
                new Command(LABEL, ifzero).emit(),
                new Command(SUB, 'B', 'B').emit(),
                new Command(LABEL, end).emit()
        );
    }

    private Stream<Command> nonzero(Stream<Command> v, Label end) {
        return concat(v, new Command(JZERO, 'C', end).emit());
    }

    public Stream<Command> equal(Stream<Command> v1, Stream<Command> v2) {
        Label success = new Label();
        return concat(v1, v2,
                new Command(COPY, 'D', 'C').emit(),
                new Command(SUB, 'D', 'B').emit(),
                new Command(SUB, 'B', 'C').emit(),
                new Command(ADD, 'B', 'D').emit(),
                new Command(JZERO, 'B', success).emit(),
                new Command(JUMP, jump).emit(),
                new Command(LABEL, success).emit());
    }

    public Stream<Command> nequal(Stream<Command> v1, Stream<Command> v2) {
        return concat(v1, v2,
                new Command(COPY, 'D', 'C').emit(),
                new Command(SUB, 'D', 'B').emit(),
                new Command(SUB, 'B', 'C').emit(),
                new Command(ADD, 'B', 'D').emit(),
                new Command(JZERO, 'B', jump).emit());
    }

    public Stream<Command> gt(Stream<Command> v1, Stream<Command> v2) {
        return concat(v1, v2,
                new Command(SUB, 'B', 'C').emit(),
                new Command(JZERO, 'B', jump).emit());
    }

    public Stream<Command> lt(Stream<Command> v1, Stream<Command> v2) {
        return concat(v1, v2,
                new Command(SUB, 'C', 'B').emit(),
                new Command(JZERO, 'C', jump).emit());
    }

    public Stream<Command> ge(Stream<Command> v1, Stream<Command> v2) {
        Label success = new Label();
        return concat(v1, v2,
                new Command(SUB, 'C', 'B').emit(),
                new Command(JZERO, 'C', success).emit(),
                new Command(JUMP, jump).emit(),
                new Command(LABEL, success).emit());
    }

    public Stream<Command> le(Stream<Command> v1, Stream<Command> v2) {
        Label success = new Label();
        return concat(v1, v2,
                new Command(SUB, 'B', 'C').emit(),
                new Command(JZERO, 'B', success).emit(),
                new Command(JUMP, jump).emit(),
                new Command(LABEL, success).emit());
    }

    public Stream<Command> number(long n) {
        return number(reg, n);
    }


    private static Stream<Command> number(char reg, long n) {
        return concat(new Command(SUB, reg, reg).emit(), n > 0 ? numberLoop(reg, n) : concat());
    }

    private static Stream<Command> numberLoop(char reg, long n) {
        Stream<Command> front, back;
        if (n > 1) {
            front = concat(numberLoop(reg, n >> 1), new Command(ADD, reg, reg).emit());
        } else {
            front = concat();
        }
        if ((n & 1) == 1) {
            back = new Command(INC, reg).emit();
        } else {
            back = concat();
        }
        return concat(front, back);
    }

    public Stream<Command> load(Stream<Command> id) {
        return concat(id, new Command(LOAD, reg).emit());
    }

    public Stream<Command> scalar(String id) {
        if (!symbols.containsKey(id)) {
            return error("Niezadeklarowana zmienna");
        }
        AboutSymbol about = symbols.get(id);
        if (about.isArray) {
            return error("Zmienna tablicowa użyta jako skalarna");
        }
        if (about.isIter && mutable) {
            return error("Zapis do zmiennej pętli");
        }
        if (mutable) {
            toBeInitialised = about;
        } else if (!about.initialised) {
            return error("Niezainicjalizowana zmienna");
        }
        return number('A', about.address);
    }

    public Stream<Command> arrayVar(String tab, String var) {
        AboutSymbol about = aboutArray(tab);
        boolean oldMutable = mutable;
        mutable = false;
        Stream<Command> result = concat(
                scalar(var),
                new Command(LOAD, 'A').emit(),
                number(reg, about.firstIndex),
                new Command(SUB, 'A', reg).emit(),
                number(reg, about.address),
                new Command(ADD, 'A', reg).emit());
        mutable = oldMutable;
        return result;
    }

    public Stream<Command> arrayNum(String tab, long index) {
        AboutSymbol about = aboutArray(tab);
        if (index < about.firstIndex || about.lastIndex < index) {
            return error("Indeks poza zakresem tablicy");
        }
        return number('A', about.address + index - about.firstIndex);
    }

    private AboutSymbol aboutArray(String tab) {
        if (!symbols.containsKey(tab)) {
            return error("Niezadeklarowana zmienna");
        }
        AboutSymbol about = symbols.get(tab);
        if (!about.isArray) {
            return error("Zmienna skalarna użyta jako tablicowa");
        }
        return about;
    }
}