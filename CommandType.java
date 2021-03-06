public interface CommandType {
    int LABEL = 0,
            GET = 1,
            PUT = 2,
            LOAD = 3,
            STORE = 4,
            COPY = 5,
            ADD = 6,
            SUB = 7,
            INC = 8,
            DEC = 9,
            HALF = 10,
            JUMP = 11,
            JZERO = 12,
            JODD = 13;
    String[] name = {
            "LABEL",
            "GET",
            "PUT",
            "LOAD",
            "STORE",
            "COPY",
            "ADD",
            "SUB",
            "INC",
            "DEC",
            "HALF",
            "JUMP",
            "JZERO",
            "JODD"};
}