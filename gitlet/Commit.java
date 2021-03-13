package gitlet;

import java.util.ArrayList;
import java.util.Date;
import java.io.File;
import java.util.HashMap;
import java.io.Serializable;



/** Class for commits made in GitLet. **/
public class Commit implements Serializable {
    private String sha;
    private String message;
    private Date timeDate;
    private String parentSha;
    private String parentSha2; //array of ancestor shas, starting with most recent (self's, then parent's)
    private boolean wasMerged;
    private HashMap<String, String> files; //names as keys, sha1 of blobs as values

    /** Initializes first, empty commit with message of "initial commit"
     * and timestamp of 00:00:00 UTC, Thursday, 1 January 1970. **/
    public Commit() {
        this.sha = Utils.sha1( Utils.serialize(this));
        this.message = "initial commit";
        this.timeDate = new Date(0);
        this.parentSha = null;
        this.files = null;
        this.parentSha2 = null;
        this.wasMerged = false;
    }
    /** Initializes commit given a message, parent SHA-1,
     * and map of files, at time and date commit occurs. **/
    public Commit(String message, String parentSha, HashMap files) {
        this.sha = null;
        this.message = message;
        this.timeDate = new Date();
        this.parentSha = parentSha;
        this.files = files;
        this.parentSha2 = null;
        this.wasMerged = false;
    }

    /** Saves the current Commit to a file with the name being its SHA-1. **/
    public void saveCommit() {
        File com = new File(".gitlet/commits/" + this.sha);
        Utils.writeObject(com, this);
    }

    /** Given a SHA-1 id, reads the file of that name and return the Commit Object. **/
    public static Commit writeCommit(String sha1) {
        File c = new File(".gitlet/commits/" + sha1);
        Commit com = Utils.readObject(c, Commit.class);
        return com;
    }

    public Commit getParentCommit() {
        return writeCommit(this.parentSha);
    }

    public static String shaToParentSha(String sha) {
        Commit c = writeCommit(sha);
        String p =  c.getParentSha();
        return p;
    }
    public String getParentSha2() {
        return this.parentSha2;
    }

    public boolean isWasMerged() {
        return this.wasMerged;
    }

    public void setParentSha2(String sha) {
        this.parentSha2 = sha;
    }

    public void setWasMerged() {
        this.wasMerged = true;
    }

    public void setSha(String sha1) {
        this.sha = sha1;
    }

    public String getSha() {return this.sha; }

    public String getParentSha() { return this.parentSha; }

    public Date getTimeDate() { return this.timeDate; }

    public String getMessage() { return this.message; }

    public HashMap<String, String> getFiles() { return this.files;}
<<<<<<< HEAD
}
=======
}
>>>>>>> 2abddd335d450ccc305be0cbfe717cb43b9cb205
