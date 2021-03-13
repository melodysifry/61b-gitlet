package gitlet;
import java.io.Serializable;

public class Blob implements Serializable{
    private String contents;
    private String sha1;
    private String name;

    public Blob(String contents, String sha1, String name) {
        this.contents = contents;
        this.sha1 = sha1;
        this.name = name;
    }

    public String getContents() {
        return contents;
    }

    public String getSha1() {
        return sha1;
    }

    public String getName() {
        return name;
    }
}
