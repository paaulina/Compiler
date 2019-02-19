import java.util.stream.Stream;

/**
 * import static cmd.Command.long;
 * import static cmd.Command;
 * Stream cs;
 * cmd.Command.Label label;
 * <p>
 * concat(cs, new cmd.Command(JUMP, label).emit());
 */
public class Command implements CommandType {
    private int type;
    private char reg1, reg2;
    private Label label;

    public Command(int type, char reg) {
        switch (type) {
            case DEC:
            case INC:
            case GET:
            case PUT:
            case LOAD:
            case STORE:
            case HALF:
                break;
            default:
                throw new RuntimeException("Bad command");
        }
        this.type = type;
        this.reg1 = reg;
    }

    public Command(int type, char reg1, char reg2) {
        switch (type) {
            case ADD:
            case SUB:
            case COPY:
                break;
            default:
                throw new RuntimeException("Bad command");
        }
        this.type = type;
        this.reg1 = reg1;
        this.reg2 = reg2;
    }

    public Command(int type, Label label) {
        switch (type) {
            case JUMP:
            case LABEL:
                break;
            default:
                throw new RuntimeException("Bad command");
        }
        this.type = type;
        this.label = label;
    }

    public Command(int type, char reg, Label label) {
        switch (type) {
            case JZERO:
            case JODD:
                break;
            default:
                throw new RuntimeException("Bad command");
        }
        this.type = type;
        this.reg1 = reg;
        this.label = label;
    }

    @Override
    public String toString() {
        switch (type) {
            case ADD:
            case SUB:
            case COPY:
                return name[type] + " " + reg1 + " " + reg2 + "\n";
            case DEC:
            case INC:
            case GET:
            case PUT:
            case LOAD:
            case STORE:
            case HALF:
                return name[type] + " " + reg1 + "\n";
            case JZERO:
            case JODD:
                return name[type] + " " + reg1 + " " + label.value + "\n";
            case JUMP:
                return "JUMP " + label.value + "\n";
            default:
                return "";
        }
    }

    public Stream<Command> emit() {
        return Stream.of(this);
    }

    public boolean solveLabel(long programCounter) {
        switch (type) {
            case LABEL:
                label.value = programCounter;
                return true;
            default:
                return false;
        }
    }
}
