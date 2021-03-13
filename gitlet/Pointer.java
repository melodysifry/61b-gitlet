package gitlet;

import java.util.Date;
import java.io.File;
import java.util.HashMap;
import java.io.Serializable;

public class Pointer implements Serializable {

    /** Name of the "pointer". **/
    private String name;
    /** Sha-1 that it points to. **/
    private String pointsTo;
    /** Name of current branch, only used for head pointer, null for all other pointers **/
    private String currBranch;

    public Pointer(String n, String p, String c) {
        this.name = n;
        this.pointsTo = p;
        this.currBranch = c;
    }

    public String getName() {
        return this.name;
    }

    public String getPointsTo() {
        return this.pointsTo;
    }
    public String getCurrBranch() {
        return this.currBranch;
    }

    public void update(String sha1, String curr) {
        this.pointsTo = sha1;
        this.currBranch = curr;
    }
    public void savePointer() {
        File p = new File(".gitlet/branches/" + this.name);
        Utils.writeObject(p, this);
    }

    public static Pointer writePointer(String name) {
        File p = new File(".gitlet/branches/" + name);
        Pointer point = Utils.readObject(p, Pointer.class);
        return point;
    }
}
