package madnet.channel;

public final class Result {
    public final int read;
    public final int writen;

    public Result(int read, int writen) {
        this.read = read;
        this.writen = writen;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Result))
            return false;

        Result result = (Result)obj;

        return result.read == read &&
            result.writen == writen;
    }

    @Override
    public int hashCode() {
        int hash = 164;
        hash = 31 * hash + read;

        return 31 * hash + writen;
    }

    @Override
    public String toString() {
        return "#<madnet.Channel.Result read=" + read +
            " writen=" + writen + ">";
    }

    public Result add(Result result) {
        return new Result(read + result.read, writen + result.writen);
    }
}
