package gitlet;
import java.io.File;
import java.security.MessageDigest;
import java.util.HashMap;
import java.io.IOException;
import java.util.List;
import java.util.Date;
import java.util.Formatter;


/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author
 */
public class Main {

    //static final File CWD = new File(".");

    static final File GITLET_FOLDER = new File(".gitlet");
    static final File COMMIT_FOLDER = new File(".gitlet/commits");
    static final File BLOBS_FOLDER = new File(".gitlet/blobs");
    static final File STAGING_AREA = new File(".gitlet/staging");
    static final File ADD_FOLDER = new File(".gitlet/staging/addFolder");
    static final File RM_FOLDER = new File(".gitlet/staging/rmFolder");
    static final File BRANCH_FOLDER = new File(".gitlet/branches");
    public static final Pointer head = new Pointer("head", "", "master");
    public static final Pointer master = new Pointer("master", "", null);



    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        if (args[0].equals("init")) {
            init();
        }
        else {
            if (!GITLET_FOLDER.exists()) {
                System.out.println("Not in an initialized Gitlet directory.");
                System.exit(0);
            }
            switch (args[0]) {
                case "add":
                    if (args.length == 2) {
                        add(args[1]);
                    } else {
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                    }
                    break;
                case "commit":
                    if (args.length < 2 || args[1].isEmpty()) {
                        System.out.println("Please enter a commit message.");
                    } else if (args.length == 2) {
                        commit(args);
                    } else {
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                    }
                    break;
                case "log":
                    log(args);
                    break;
                case "find":
                    if (args.length == 2) {
                        find(args);
                    } else {
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                    }
                    break;
                case "checkout":
                    checkout(args);
                    break;
                case "global-log":
                    globalLog(args);
                    break;
                case "rm":
                    if (args.length != 2) {
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                    } else {
                        rm(args[1]);
                    }
                    break;
                case "status":
                    status(args);
                    break;
                case "reset":
                    if (args.length == 2) {
                        reset(args);
                    } else {
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                    }
                    break;
                case "branch":
                    if (args.length == 2) {
                        branch(args[1]);
                    } else {
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                    }
                    break;
                case "rm-branch":
                    if (args.length == 2) {
                        rmBranch(args[1]);
                    } else {
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                    }
                    break;
                case "merge" :
                    if (args.length == 2) {
                        merge(args[1]);
                    } else {
                        System.out.println("Incorrect Operands.");
                        System.exit(0);
                    }
                    break;
                default:
                    System.out.println("No command with that name exists.");
                    System.exit(0);
            }
        }
    }


    /** Initializes a new Gitlet version-control system in the current directory. **/
    public static void init(){
        if (!GITLET_FOLDER.exists()){
            GITLET_FOLDER.mkdir();
            COMMIT_FOLDER.mkdir();
            STAGING_AREA.mkdir();
            BLOBS_FOLDER.mkdir();
            ADD_FOLDER.mkdir();
            RM_FOLDER.mkdir();
            BRANCH_FOLDER.mkdir();
            Commit firstCommit = new Commit();
            firstCommit.saveCommit();
            head.update(firstCommit.getSha(), "master");
            head.savePointer();
            master.update(firstCommit.getSha(), null);
            master.savePointer();

        }
        else {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
    }

    public static void commit(String[] args) {
        if (Utils.plainFilenamesIn(ADD_FOLDER).isEmpty() && Utils.plainFilenamesIn(RM_FOLDER).isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        String message = args[1];
        Pointer head1 = Pointer.writePointer("head");
        String branchName = head1.getCurrBranch(); //name of current branch
        Pointer branch = Pointer.writePointer(branchName); // current branch pointer
        String parentSha = head1.getPointsTo(); //sha1 id of parent commit
        Commit parent = Commit.writeCommit(parentSha);
        HashMap<String, String> hashFile = new HashMap<>();
        if (parent.getFiles() != null) {
            hashFile.putAll(parent.getFiles());
        }
        if (!Utils.plainFilenamesIn(RM_FOLDER).isEmpty()) { // if remove folder has things in it
            List<String> rmNames = Utils.plainFilenamesIn(RM_FOLDER);
            for (String r : rmNames) {
                hashFile.remove(r);
                File rmFile = new File(".gitlet/staging/rmFolder/" + r);
                rmFile.delete();
            }
            if (Utils.plainFilenamesIn(ADD_FOLDER).isEmpty()){ // if nothing in add folder
                Commit newCommit = new Commit(message, parentSha, hashFile);
                String sha = Utils.sha1(Utils.serialize(newCommit));
                newCommit.setSha(sha);
                newCommit.saveCommit();
                head1.update(newCommit.getSha(), branchName);
                head1.savePointer();
                branch.update(newCommit.getSha(), null);
                branch.savePointer();
            }
        }
        if (!Utils.plainFilenamesIn(ADD_FOLDER).isEmpty()) { // if add folder has things in it
            List<String> stagingNames = Utils.plainFilenamesIn(ADD_FOLDER); //list of names of files in add folder
            // adds names as keys and sha of blob as values into hashFile from staging add area, removes them from staging area
            for (String i : stagingNames) {
                File I_FILE = new File(".gitlet/staging/addFolder/" + i); // pointer to file in staging add area
                String blobSha = Utils.readContentsAsString(I_FILE);
                hashFile.put(I_FILE.getName(), blobSha); // adds name as key, sha as val to hashFile
                I_FILE.delete();
            }
            Commit newCommit = new Commit(message, parentSha, hashFile);
            String sha = Utils.sha1(Utils.serialize(newCommit));
            newCommit.setSha(sha);
            newCommit.saveCommit(); //saves new commit to a file named by sha1 id containing commit object in commits folder
            head1.update(newCommit.getSha(), branchName);
            head1.savePointer();//updates head pointer
            branch.update(newCommit.getSha(), null);
            branch.savePointer();//updates branch pointer
        }
    }

    /** Takes in name of file to be added, adds to staging area if that file has never been added
     *  or has been modified since last commit */
    public static void add(String a) {
        File ADD_FILE = new File(a); // pointer to file to be added in CWD
        if (!ADD_FILE.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        byte[] bytes = Utils.readContents(ADD_FILE);
        String id = Utils.sha1(bytes); //sha1 ID of file to be added
        File NEW_BLOB = new File(".gitlet/blobs/" + id);
        if (!NEW_BLOB.exists()) { //if this is a new/updated blob
            Utils.writeContents(NEW_BLOB, bytes);
        }
        File STAGING_BLOB = new File(".gitlet/staging/addFolder/" + a);
        if (STAGING_BLOB.exists() && Utils.readContentsAsString(STAGING_BLOB).equals(id)) {
            System.exit(0);
        }
        Commit c = Commit.writeCommit(Pointer.writePointer("head").getPointsTo());
        if (!c.getMessage().equals("initial commit")) {
            HashMap<String, String> files = c.getFiles();
            if (files.containsKey(a) && files.get(a).equals(id)) {
                File rmF = new File(".gitlet/staging/rmFolder/" + a);
                if (rmF.exists()) {
                    rmF.delete();
                }
                System.exit(0);
            }
        }
        Utils.writeContents(STAGING_BLOB, id);
    }

    /** Unstage or untrack and delete a given file. **/
    public static void rm(String fileName) {
        File a = new File(".gitlet/staging/addFolder/" + fileName);
        File r = new File(".gitlet/staging/rmFolder/" + fileName);
        File wd = new File(fileName);
        Pointer head1 = Pointer.writePointer("head");
        String sha = head1.getPointsTo();
        Commit c = Commit.writeCommit(sha);
        HashMap<String, String> commitFiles = c.getFiles();
        if (a.exists()){
            a.delete();
<<<<<<< HEAD
        }
        else if (commitFiles == null || (commitFiles.get(fileName) == null && !a.exists())) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        else if (commitFiles.get(fileName) != null) {
            if (wd.exists()) {
                wd.delete();
            }
            Utils.writeContents(r, commitFiles.get(fileName));
        }
=======
        }
        else if (commitFiles == null || (commitFiles.get(fileName) == null && !a.exists())) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        else if (commitFiles.get(fileName) != null) {
            if (wd.exists()) {
                wd.delete();
            }
            Utils.writeContents(r, commitFiles.get(fileName));
        }
>>>>>>> 2abddd335d450ccc305be0cbfe717cb43b9cb205

    }

    /** Prints out a log of commits in repository starting from the current head,
     * going to the initial commit. **/
    public static void log(String[] args) {
        Pointer head1 = Pointer.writePointer("head");
        String sha = head1.getPointsTo();
        while (sha != null) {
            Commit c = Commit.writeCommit(sha);
<<<<<<< HEAD
            System.out.println("===");
            System.out.println("commit " + sha);
=======
            System.out.println("===");
            System.out.println("commit " + sha);
            if (c.isWasMerged()) {
                String parent1 = c.getParentSha();
                String parent2 = c.getParentSha2();
                System.out.println("Merge: " + parent1.substring(0, 7) + " " + parent2.substring(0, 7));
            }
            Date date =  c.getTimeDate();
            System.out.println(String.format("Date: %1$ta %1$tb %1$te %1$tT %1$tY %1$tz", date));
            System.out.println(c.getMessage());
            System.out.println();
            sha = c.getParentSha();
        }
    }

    /** Prints out a log of all the commits that exist in a repository. **/
    public static void globalLog(String[] args) {
        if (args.length != 1) {
            System.out.println("Incorrect Operands.");
            System.exit(0);
        }
        List<String> commits = Utils.plainFilenamesIn(COMMIT_FOLDER);
        for (String s : commits) {
            Commit c = Commit.writeCommit(s);
            System.out.println("===");
            System.out.println("commit " + s);
>>>>>>> 2abddd335d450ccc305be0cbfe717cb43b9cb205
            if (c.isWasMerged()) {
                String parent1 = c.getParentSha();
                String parent2 = c.getParentSha2();
                System.out.println("Merge: " + parent1.substring(0, 7) + " " + parent2.substring(0, 7));
            }
            Date date =  c.getTimeDate();
            System.out.println(String.format("Date: %1$ta %1$tb %1$te %1$tT %1$tY %1$tz", date));
            System.out.println(c.getMessage());
            System.out.println();
            sha = c.getParentSha();
        }
    }

    /** Prints out a log of all the commits that exist in a repository. **/
    public static void globalLog(String[] args) {
        if (args.length != 1) {
            System.out.println("Incorrect Operands.");
            System.exit(0);
        }
        List<String> commits = Utils.plainFilenamesIn(COMMIT_FOLDER);
        for (String s : commits) {
            Commit c = Commit.writeCommit(s);
            System.out.println("===");
            System.out.println("commit " + s);
            if (c.isWasMerged()) {
                String parent1 = c.getParentSha();
                String parent2 = c.getParentSha2();
                System.out.println("Merge: " + parent1.substring(0, 7) + " " + parent2.substring(0, 7));
            }
            Date date =  c.getTimeDate();
            System.out.println(String.format("Date: %1$ta %1$tb %1$te %1$tT %1$tY %1$tz", date));
            System.out.println(c.getMessage());
            System.out.println();
        }
    }

    /** Prints out the ids of all the commits with a given message. **/
    public static void find(String[] args) {
        String mes = args[1];
        File coms = new File(".gitlet/commits");
        List<String> commits = Utils.plainFilenamesIn(coms);
        int count = 0;
        for (String f : commits) {
            Commit c = Commit.writeCommit(f);
            if (c.getMessage().equals(mes)) {
                System.out.println(c.getSha());
                count += 1;
            }
        }
        if (count == 0) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    /** Given a file name, adds file in most recent commit, overwriting if necessary, to working directory.
     * Given a checkout id and file name adds file from that checkout to working directory.
     * Given a branch name, takes all the files in the head of that branch and adds them to working directory.
     **/
    public static void checkout(String[] args) {
        if (args.length == 3) { // for checkout -- [file name]
            if (!args[1].equals("--")) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            Pointer head1 = Pointer.writePointer("head");
            String sha = head1.getPointsTo();
            Commit c = Commit.writeCommit(sha);
            HashMap<String, String> files = c.getFiles();
            if (files.get(args[2]) == null) {
                System.out.println("File does not exist in that commit.");
                System.exit(0);
            }
            String blobSha = files.get(args[2]);
            File blobFile = new File(".gitlet/blobs/" + blobSha);
            byte[] contents = Utils.readContents(blobFile);
            File theFile = new File(args[2]);
            Utils.writeContents(theFile, contents);
        }

        if (args.length == 4) { // for checkout [commit id] -- [file name]
            if (!args[2].equals("--")) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            String comID = args[1];
            if (comID.length() < 40 && comID.length() > 5 ) {
                List<String> existingC = Utils.plainFilenamesIn(COMMIT_FOLDER);
                for (String s : existingC) {
                    if (s.startsWith(comID)) {
                        comID = s;
                        break;
                    }
                }
            }
            File cFile = new File(".gitlet/commits/" + comID);
            if (!cFile.exists()) {
                System.out.println("No commit with that id exists.");
                System.exit(0);
            }
            Commit c = Commit.writeCommit(comID);
            HashMap<String, String> files = c.getFiles();
            if (files.get(args[3]) == null) {
                System.out.println("File does not exist in that commit.");
                System.exit(0);
            }
            String blobSha = files.get(args[3]);
            File blobFile = new File(".gitlet/blobs/" + blobSha);
            byte[] contents = Utils.readContents(blobFile);
            File theFile = new File(args[3]);
            Utils.writeContents(theFile, contents);
        }
        if (args.length == 2) { // for checkout [branch name]
            File br = new File(".gitlet/branches/" + args[1]);
            if (!br.exists()) {
                System.out.println("No such branch exists.");
                System.exit(0);
            }
            Pointer branch = Utils.readObject(br, Pointer.class); //given branch
            Pointer head1 = Pointer.writePointer("head");
            Pointer currBranch = Pointer.writePointer(head1.getCurrBranch()); //current branch
            if (currBranch.getName().equals(branch.getName())) {
                System.out.println("No need to checkout the current branch.");
                System.exit(0);
            }
            String comSha = branch.getPointsTo(); //given branch's commit sha
            Commit com = Commit.writeCommit(comSha); //given branch's commit
            HashMap<String, String> comFiles = com.getFiles(); //files in given branch's commit
            String headSha = head1.getPointsTo();
            Commit headCom = Commit.writeCommit(headSha);
            HashMap<String, String> headFiles = headCom.getFiles(); //files in current branch's commit
            File curD = new File("./");
            List<String> curDFiles = Utils.plainFilenamesIn(curD);
            for (String s : curDFiles) {
                if ((headFiles == null && comFiles != null) || (!headFiles.containsKey(s) && !comFiles.containsKey(s))){
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
                File sFile = new File(s);
                sFile.delete();
            }
            if (comFiles != null) {
                for (String f : comFiles.keySet()) {
                    String blobSha = comFiles.get(f);
                    File blobFile = new File(".gitlet/blobs/" + blobSha);
                    byte[] contents = Utils.readContents(blobFile);
                    File theFile = new File(f);
                    Utils.writeContents(theFile, contents);
                }
            }
            if (!Utils.plainFilenamesIn(ADD_FOLDER).isEmpty()) {
                for (String a : Utils.plainFilenamesIn(ADD_FOLDER)) {
                    File add = new File(".gitlet/staging/addFolder/" + a);
                    add.delete();
                }
            }
            if (!Utils.plainFilenamesIn(RM_FOLDER).isEmpty()) {
                for (String r : Utils.plainFilenamesIn(RM_FOLDER)) {
                    File remove = new File(".gitlet/staging/rmFolder/" + r);
                    remove.delete();
                }
            }
            head1.update(comSha, branch.getName());
            branch.update(comSha, null);
            branch.savePointer();
            head1.savePointer();
        }
    }

    /** Displays what branches exist and what files are staged for addition or removal. **/
    public static void status(String[] args) {
        if (args.length != 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        Pointer head1 = Pointer.writePointer("head");
        String currB = head1.getCurrBranch();
        System.out.println("=== Branches ===");
        List<String> branch = Utils.plainFilenamesIn(BRANCH_FOLDER);
        branch.sort(String::compareTo);
        for (String s : branch) {
            if (s.equals(currB)) {
                System.out.println("*" + s);
                continue;
            }
            if (s.equals("head")) {
                continue;
            }
            else {
                System.out.println(s);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        if (!Utils.plainFilenamesIn(ADD_FOLDER).isEmpty()) {
            List<String> names = Utils.plainFilenamesIn(ADD_FOLDER);
            names.sort(String::compareTo);
            for (String s : names) {
                System.out.println(s);
            }
        }
<<<<<<< HEAD
=======
    }

    /** Prints out the ids of all the commits with a given message. **/
    public static void find(String[] args) {
        String mes = args[1];
        File coms = new File(".gitlet/commits");
        List<String> commits = Utils.plainFilenamesIn(coms);
        int count = 0;
        for (String f : commits) {
            Commit c = Commit.writeCommit(f);
            if (c.getMessage().equals(mes)) {
                System.out.println(c.getSha());
                count += 1;
            }
        }
        if (count == 0) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    /** Given a file name, adds file in most recent commit, overwriting if necessary, to working directory.
     * Given a checkout id and file name adds file from that checkout to working directory.
     * Given a branch name, takes all the files in the head of that branch and adds them to working directory.
     **/
    public static void checkout(String[] args) {
        if (args.length == 3) { // for checkout -- [file name]
            if (!args[1].equals("--")) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            Pointer head1 = Pointer.writePointer("head");
            String sha = head1.getPointsTo();
            Commit c = Commit.writeCommit(sha);
            HashMap<String, String> files = c.getFiles();
            if (files.get(args[2]) == null) {
                System.out.println("File does not exist in that commit.");
                System.exit(0);
            }
            String blobSha = files.get(args[2]);
            File blobFile = new File(".gitlet/blobs/" + blobSha);
            byte[] contents = Utils.readContents(blobFile);
            File theFile = new File(args[2]);
            Utils.writeContents(theFile, contents);
        }

        if (args.length == 4) { // for checkout [commit id] -- [file name]
            if (!args[2].equals("--")) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            String comID = args[1];
            if (comID.length() < 40 && comID.length() > 5 ) {
                List<String> existingC = Utils.plainFilenamesIn(COMMIT_FOLDER);
                for (String s : existingC) {
                    if (s.startsWith(comID)) {
                        comID = s;
                        break;
                    }
                }
            }
            File cFile = new File(".gitlet/commits/" + comID);
            if (!cFile.exists()) {
                System.out.println("No commit with that id exists.");
                System.exit(0);
            }
            Commit c = Commit.writeCommit(comID);
            HashMap<String, String> files = c.getFiles();
            if (files.get(args[3]) == null) {
                System.out.println("File does not exist in that commit.");
                System.exit(0);
            }
            String blobSha = files.get(args[3]);
            File blobFile = new File(".gitlet/blobs/" + blobSha);
            byte[] contents = Utils.readContents(blobFile);
            File theFile = new File(args[3]);
            Utils.writeContents(theFile, contents);
        }
        if (args.length == 2) { // for checkout [branch name]
            File br = new File(".gitlet/branches/" + args[1]);
            if (!br.exists()) {
                System.out.println("No such branch exists.");
                System.exit(0);
            }
            Pointer branch = Utils.readObject(br, Pointer.class); //given branch
            Pointer head1 = Pointer.writePointer("head");
            Pointer currBranch = Pointer.writePointer(head1.getCurrBranch()); //current branch
            if (currBranch.getName().equals(branch.getName())) {
                System.out.println("No need to checkout the current branch.");
                System.exit(0);
            }
            String comSha = branch.getPointsTo(); //given branch's commit sha
            Commit com = Commit.writeCommit(comSha); //given branch's commit
            HashMap<String, String> comFiles = com.getFiles(); //files in given branch's commit
            String headSha = head1.getPointsTo();
            Commit headCom = Commit.writeCommit(headSha);
            HashMap<String, String> headFiles = headCom.getFiles(); //files in current branch's commit
            File curD = new File("./");
            List<String> curDFiles = Utils.plainFilenamesIn(curD);
            for (String s : curDFiles) {
                if ((headFiles == null && comFiles != null) || (!headFiles.containsKey(s) && !comFiles.containsKey(s))){
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
                File sFile = new File(s);
                sFile.delete();
            }
            if (comFiles != null) {
                for (String f : comFiles.keySet()) {
                    String blobSha = comFiles.get(f);
                    File blobFile = new File(".gitlet/blobs/" + blobSha);
                    byte[] contents = Utils.readContents(blobFile);
                    File theFile = new File(f);
                    Utils.writeContents(theFile, contents);
                }
            }
            if (!Utils.plainFilenamesIn(ADD_FOLDER).isEmpty()) {
                for (String a : Utils.plainFilenamesIn(ADD_FOLDER)) {
                    File add = new File(".gitlet/staging/addFolder/" + a);
                    add.delete();
                }
            }
            if (!Utils.plainFilenamesIn(RM_FOLDER).isEmpty()) {
                for (String r : Utils.plainFilenamesIn(RM_FOLDER)) {
                    File remove = new File(".gitlet/staging/rmFolder/" + r);
                    remove.delete();
                }
            }
            head1.update(comSha, branch.getName());
            branch.update(comSha, null);
            branch.savePointer();
            head1.savePointer();
        }
    }

    /** Displays what branches exist and what files are staged for addition or removal. **/
    public static void status(String[] args) {
        if (args.length != 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        Pointer head1 = Pointer.writePointer("head");
        String currB = head1.getCurrBranch();
        System.out.println("=== Branches ===");
        List<String> branch = Utils.plainFilenamesIn(BRANCH_FOLDER);
        branch.sort(String::compareTo);
        for (String s : branch) {
            if (s.equals(currB)) {
                System.out.println("*" + s);
                continue;
            }
            if (s.equals("head")) {
                continue;
            }
            else {
                System.out.println(s);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        if (!Utils.plainFilenamesIn(ADD_FOLDER).isEmpty()) {
            List<String> names = Utils.plainFilenamesIn(ADD_FOLDER);
            names.sort(String::compareTo);
            for (String s : names) {
                System.out.println(s);
            }
        }
>>>>>>> 2abddd335d450ccc305be0cbfe717cb43b9cb205
        System.out.println();
        System.out.println("=== Removed Files ===");
        if (!Utils.plainFilenamesIn(RM_FOLDER).isEmpty()) {
            List<String> names = Utils.plainFilenamesIn(RM_FOLDER);
            names.sort(String::compareTo);
            for (String s : names) {
                System.out.println(s);
            }
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        /** String sha = head1.getPointsTo();
         Commit c = Commit.writeCommit(sha);
         HashMap<String, String> files = c.getFiles();
         List<String> changed = new ArrayList<String>();
         if (!files.isEmpty()) {
         for (String s : files.keySet()) {
         File name = new File(s);
         File rm = new File(".gitlet/staging/rmFolder/" + s);
         File ad = new File(".gitlet/staging/addFolder/" + s);
         if (!name.exists() && !rm.exists()) {
         changed.add(s + "(deleted)");
         }
         String blobSha = files.get(s);
         byte[] bytes = Utils.readContents(name);
         String id = Utils.sha1(bytes);
         if (id != blobSha && !ad.exists()) {
         changed.add(s + "(modified)");
         }
         if (ad.exists() && !name.exists()) {
         changed.add(s + "(deleted)");
         }
         if (ad.exists() && Utils.readContentsAsString(ad) != id) {
         changed.add(s + "modified");
         }
         }
         changed.sort(String::compareTo);
         for (String j : changed) {
         System.out.println(j);
         }
         } **/
        System.out.println();
        System.out.println("=== Untracked Files ===");
        // files not in wd but not in recent commit and not staged for addition
    }
<<<<<<< HEAD

    /** Checks out all the files tracked by a given commit. **/
    public static void reset(String[] args) {
        String comID = args[1];
        Pointer head1 = Pointer.writePointer("head");
        if (comID.length() < 40 && comID.length() > 5) {
            List<String> existingC = Utils.plainFilenamesIn(COMMIT_FOLDER);
            for (String s : existingC) {
                if (s.startsWith(comID)) {
                    comID = s;
                    break;
                }
            }
        }
        File commitCheck = new File(".gitlet/commits/" + comID);
        if (!commitCheck.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Commit c = Commit.writeCommit(comID);
        HashMap<String, String> files = c.getFiles();
        File curD = new File("./");
        List<String> currFiles = Utils.plainFilenamesIn(curD);
        Commit headCommit = Commit.writeCommit(head1.getPointsTo());
        HashMap<String, String> headFiles = headCommit.getFiles();
        for (String s : currFiles) {
            File ADD_FILE = new File(s);
            byte[] bytes = Utils.readContents(ADD_FILE);
            String id = Utils.sha1(bytes); //sha1 ID of file to be added
            File NEW_BLOB = new File(".gitlet/blobs/" + id);
            if (!NEW_BLOB.exists()) { //if this is a new/updated blob
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
            ADD_FILE.delete();
        }
        for (String n : files.keySet()) {
            String blobSha = files.get(n);
            File blobFile = new File(".gitlet/blobs/" + blobSha);
            byte[] contents = Utils.readContents(blobFile);
            File theFile = new File(n);
            Utils.writeContents(theFile, contents);
        }
        if (!Utils.plainFilenamesIn(ADD_FOLDER).isEmpty()) {
            for (String a : Utils.plainFilenamesIn(ADD_FOLDER)) {
                File add = new File(".gitlet/staging/addFolder/" + a);
                add.delete();
            }
        }
        if (!Utils.plainFilenamesIn(RM_FOLDER).isEmpty()) {
            for (String r : Utils.plainFilenamesIn(RM_FOLDER)) {
                File remove = new File(".gitlet/staging/rmFolder/" + r);
                remove.delete();
            }
        }
        head1.update(comID, head1.getCurrBranch());
        Pointer currBranch = Pointer.writePointer(head1.getCurrBranch());
        currBranch.update(comID, null);
        currBranch.savePointer();
        head1.savePointer();
    }

    public static void branch(String a) {
        //If a branch with the given name already exists, print the error message
        List<String> branches = Utils.plainFilenamesIn(BRANCH_FOLDER);
        if (branches.contains(a)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        // Creates a new branch with the given name, and points it at the current head node.
        Pointer head1 = Pointer.writePointer("head");
        Pointer newBranch = new Pointer(a, head1.getPointsTo(), null);
        newBranch.savePointer();
    }

    public static void rmBranch(String a) {
        File toDelete = new File(".gitlet/branches/" + a);
        if (!toDelete.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        Pointer head1 = Pointer.writePointer("head");
        if (head1.getCurrBranch().equals(a)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        toDelete.delete();
    }

    public static void merge(String a) {
        boolean isConflict = false;
        if (!Utils.plainFilenamesIn(ADD_FOLDER).isEmpty() || !Utils.plainFilenamesIn(RM_FOLDER).isEmpty()) {
            System.out.println("You have uncommited changes.");
            System.exit(0);
        }
        File branch = new File(".gitlet/branches/" + a);
        if (!branch.exists()){
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        Pointer givBranch = Pointer.writePointer(a); //given branch
        Pointer head1 = Pointer.writePointer("head");
        if (a.equals(head1.getCurrBranch())) {
            System.out.println("Cannot merge a branch with itself");
            System.exit(0);
        }
        Pointer currBranch = Pointer.writePointer(head1.getCurrBranch()); //current branch
        String currSha = currBranch.getPointsTo(); //current branch's commit's sha1
        String givenSha = givBranch.getPointsTo();//given branch's commit's sha1
        String givenShaHold = givBranch.getPointsTo();
        String splitPointSha = null; //split point's commit's sha1
        int i = 0;
        while (currSha != null) {
            Commit c = Commit.writeCommit(currSha);
            givenSha = givenShaHold;
            while (givenSha != null) {
                Commit g = Commit.writeCommit(givenSha);
                if (currSha.equals(givenSha)) {
                    splitPointSha = currSha;
                    i += 1;
                    break;
                }
                else if (g.getParentSha2() != null && c.getParentSha2().equals(g.getParentSha())) {
                    splitPointSha = g.getParentSha();
                    i += 1;
                    break;
                }
                else if (g.getParentSha2() != null && g.getParentSha2().equals(c.getParentSha())) {
                    splitPointSha = c.getParentSha();
                    i += 1;
                    break;
                }

                givenSha = g.getParentSha();
            }
            if (i > 0) {
                break;
            }
            currSha = c.getParentSha();
        }
        if (splitPointSha.equals(givBranch.getPointsTo())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (splitPointSha.equals(head1.getPointsTo())) {
            String[] checkoutArgs = {"checkout", a};
            checkout(checkoutArgs);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
        Commit split = Commit.writeCommit(splitPointSha);
        HashMap<String, String> splitFiles = split.getFiles();
        Commit c = Commit.writeCommit(head1.getPointsTo());
        Commit g = Commit.writeCommit(givenSha);
        HashMap<String, String> currFiles = c.getFiles(); // names as keys, sha1s of blobs as values
        HashMap<String, String> givFiles = g.getFiles();
        File curD = new File ("./");
        List<String> wdFiles = Utils.plainFilenamesIn(curD);
        for (String s : wdFiles) {
            if (!currFiles.containsKey(s)) { //if this is a new/updated blob
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        //1. any files that've been modified in the given branch since split point, but not modified in current branch
        //since split point are changed to their version in given branch (checked out from the commit at front of giv branch
        //these files will be automatically staged

        //files with same name between currFiles, splitFiles, and givFiles,
        // and same val between currFiles and splitFiles but not givFiles
        for (String name : givFiles.keySet()) {
            String sFileSha = splitFiles.get(name);
            String cFileSha = currFiles.get(name);
            String gFileSha = givFiles.get(name);
            if (sFileSha != null && cFileSha != null) {
                if (!gFileSha.equals(sFileSha) && cFileSha.equals(sFileSha)) {
                    checkout(new String[]{"checkout", givBranch.getPointsTo(), "--", name});
                    add(name);
                } else if (!cFileSha.equals(sFileSha) && gFileSha.equals(sFileSha)) {
                    continue;
                } else if (cFileSha.equals(gFileSha)) {
                    continue;
                } else if (!cFileSha.equals(sFileSha) && !cFileSha.equals(gFileSha) && !gFileSha.equals(sFileSha)) {
                    isConflict = true;
                    mergeConflict(name, gFileSha, cFileSha);
                }
            } else if (sFileSha == null && cFileSha == null) {
                checkout(new String[]{"checkout", givBranch.getPointsTo(), "--", name});
                add(name);
            } else if (sFileSha == null && !cFileSha.equals(gFileSha)) {
                isConflict = true;
                mergeConflict(name, gFileSha, cFileSha);
            } else if (cFileSha == null && !sFileSha.equals(gFileSha)) {
                isConflict = true;
                mergeConflict(name, gFileSha, cFileSha);
            }
        }
        for (String name : currFiles.keySet()) {
            String sFileSha = splitFiles.get(name);
            String cFileSha = currFiles.get(name);
            String gFileSha = givFiles.get(name);
            if (sFileSha == null && gFileSha == null) {
                continue;
            }
            else if (gFileSha == null && sFileSha.equals(cFileSha)) {
                rm(name);
            } else if (gFileSha == null && !sFileSha.equals(cFileSha)) {
                isConflict = true;
                mergeConflict(name, gFileSha, cFileSha);
            }
        }
        mergeCommit("Merged " + a + " into " + currBranch.getName() + ".", a);
        if (isConflict) {
            System.out.println("Encountered a merge conflict.");
        }

        //2. any files modified in curr branch but not modified in given branch since split point stay as they are
        //3. any files modified in both curr and given branch in same way are left unchanged
        //4. any files not at split point and present only in given branch should be checked out and staged
        //5. any files present at split point, unmodified in curr branch, and absent in given branch should be removed
        //& untracked
        //6. any files at split point, unmodified in giv branch, & absent in curr branch, should remain absent
        //7. any files modified in DIFF WAYS in curr & giv branch are IN CONFLICT
    }

    public static void mergeCommit(String message, String branch) {
        Pointer head1 = Pointer.writePointer("head");
        Pointer givenBranch = Pointer.writePointer(branch);
        Pointer currBranch = Pointer.writePointer(head1.getCurrBranch());
        String parentSha = head1.getPointsTo(); //sha1 id of parent commit
        String parentSha2 = givenBranch.getPointsTo();
        Commit parent = Commit.writeCommit(parentSha);
        HashMap<String, String> hashFile = new HashMap<>();
        if (parent.getFiles() != null) {
            hashFile.putAll(parent.getFiles());
        }
        if (!Utils.plainFilenamesIn(RM_FOLDER).isEmpty()) { // if remove folder has things in it
            List<String> rmNames = Utils.plainFilenamesIn(RM_FOLDER);
            for (String r : rmNames) {
                hashFile.remove(r);
                File rmFile = new File(".gitlet/staging/rmFolder/" + r);
                rmFile.delete();
            }
            if (Utils.plainFilenamesIn(ADD_FOLDER).isEmpty()){ // if nothing in add folder
                Commit newCommit = new Commit(message, parentSha, hashFile);
                String sha = Utils.sha1(Utils.serialize(newCommit));
                newCommit.setSha(sha);
                newCommit.setWasMerged();
                newCommit.setParentSha2(parentSha2);
                newCommit.saveCommit();
                head1.update(newCommit.getSha(), head1.getCurrBranch());
                head1.savePointer();
                currBranch.update(newCommit.getSha(), null);
                currBranch.savePointer();
            }
        }
        if (!Utils.plainFilenamesIn(ADD_FOLDER).isEmpty()) { // if add folder has things in it
            List<String> stagingNames = Utils.plainFilenamesIn(ADD_FOLDER); //list of names of files in add folder
            // adds names as keys and sha of blob as values into hashFile from staging add area, removes them from staging area
            for (String i : stagingNames) {
                File I_FILE = new File(".gitlet/staging/addFolder/" + i); // pointer to file in staging add area
                String blobSha = Utils.readContentsAsString(I_FILE);
                hashFile.put(I_FILE.getName(), blobSha); // adds name as key, sha as val to hashFile
                I_FILE.delete();
            }
            Commit newCommit = new Commit(message, parentSha, hashFile);
            String sha = Utils.sha1(Utils.serialize(newCommit));
            newCommit.setSha(sha);
            newCommit.setParentSha2(parentSha2);
            newCommit.setWasMerged();
            newCommit.saveCommit(); //saves new commit to a file named by sha1 id containing commit object in commits folder
            head1.update(newCommit.getSha(), head1.getCurrBranch());
            head1.savePointer();//updates head pointer
            currBranch.update(newCommit.getSha(), null);
            currBranch.savePointer();//updates branch pointer
        }
    }

    public static void mergeConflict(String fileName, String givenID, String currID) {
        File wdFile = new File(fileName);
        if (givenID != null && currID == null) {
            File givenFile = new File(".gitlet/blobs/" + givenID);
            String givenContents = Utils.readContentsAsString(givenFile);
            Utils.writeContents(wdFile, "<<<<<<< HEAD" + System.lineSeparator() + "======="
                    + givenContents + ">>>>>>>"
                    + System.lineSeparator());
            add(fileName);
        }
        else if (currID != null && givenID == null) {
            File currentFile = new File(".gitlet/blobs/" + currID);
            String currentContents = Utils.readContentsAsString(currentFile);
            Utils.writeContents(wdFile, "<<<<<<< HEAD" + System.lineSeparator()
                    + currentContents + "=======" + System.lineSeparator()
                    + ">>>>>>>" + System.lineSeparator());
            add(fileName);
        }
        else {
            File givenFile = new File(".gitlet/blobs/" + givenID);
            String givenContents = Utils.readContentsAsString(givenFile);
            File currentFile = new File(".gitlet/blobs/" + currID);
            String currentContents = Utils.readContentsAsString(currentFile);
            Utils.writeContents(wdFile, "<<<<<<< HEAD" + System.lineSeparator()
                    + currentContents + "======="
                    + System.lineSeparator() + givenContents + ">>>>>>>" + System.lineSeparator());
            add(fileName);
        }
    }

    /**private static String splitPointHelper(String currSha, String givenSha) {
        if (currSha == null || givenSha == null) {
            return null;
        }
        else if (currSha.equals(givenSha)) {
            return currSha;
        }
        Commit c = Commit.writeCommit(currSha);
        Commit g = Commit.writeCommit(givenSha);
        if (g.getParentSha2() != null) {
            splitPointHelper(currSha, g.getParentSha2());
        }
        if (c.getParentSha2() != null) {
            splitPointHelper(c.getParentSha2(), givenSha);
        }
        splitPointHelper(c.getParentSha(), givenSha);
        splitPointHelper(currSha, g.getParentSha());
        return null;
    } **/
=======

    /** Checks out all the files tracked by a given commit. **/
    public static void reset(String[] args) {
        String comID = args[1];
        Pointer head1 = Pointer.writePointer("head");
        if (comID.length() < 40 && comID.length() > 5) {
            List<String> existingC = Utils.plainFilenamesIn(COMMIT_FOLDER);
            for (String s : existingC) {
                if (s.startsWith(comID)) {
                    comID = s;
                    break;
                }
            }
        }
        File commitCheck = new File(".gitlet/commits/" + comID);
        if (!commitCheck.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Commit c = Commit.writeCommit(comID);
        HashMap<String, String> files = c.getFiles();
        File curD = new File("./");
        List<String> currFiles = Utils.plainFilenamesIn(curD);
        Commit headCommit = Commit.writeCommit(head1.getPointsTo());
        HashMap<String, String> headFiles = headCommit.getFiles();
        for (String s : currFiles) {
            File ADD_FILE = new File(s);
            byte[] bytes = Utils.readContents(ADD_FILE);
            String id = Utils.sha1(bytes); //sha1 ID of file to be added
            File NEW_BLOB = new File(".gitlet/blobs/" + id);
            if (!NEW_BLOB.exists()) { //if this is a new/updated blob
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
            ADD_FILE.delete();
        }
        for (String n : files.keySet()) {
            String blobSha = files.get(n);
            File blobFile = new File(".gitlet/blobs/" + blobSha);
            byte[] contents = Utils.readContents(blobFile);
            File theFile = new File(n);
            Utils.writeContents(theFile, contents);
        }
        if (!Utils.plainFilenamesIn(ADD_FOLDER).isEmpty()) {
            for (String a : Utils.plainFilenamesIn(ADD_FOLDER)) {
                File add = new File(".gitlet/staging/addFolder/" + a);
                add.delete();
            }
        }
        if (!Utils.plainFilenamesIn(RM_FOLDER).isEmpty()) {
            for (String r : Utils.plainFilenamesIn(RM_FOLDER)) {
                File remove = new File(".gitlet/staging/rmFolder/" + r);
                remove.delete();
            }
        }
        head1.update(comID, head1.getCurrBranch());
        Pointer currBranch = Pointer.writePointer(head1.getCurrBranch());
        currBranch.update(comID, null);
        currBranch.savePointer();
        head1.savePointer();
    }

    public static void branch(String a) {
        //If a branch with the given name already exists, print the error message
        List<String> branches = Utils.plainFilenamesIn(BRANCH_FOLDER);
        if (branches.contains(a)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        // Creates a new branch with the given name, and points it at the current head node.
        Pointer head1 = Pointer.writePointer("head");
        Pointer newBranch = new Pointer(a, head1.getPointsTo(), null);
        newBranch.savePointer();
    }

    public static void rmBranch(String a) {
        File toDelete = new File(".gitlet/branches/" + a);
        if (!toDelete.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        Pointer head1 = Pointer.writePointer("head");
        if (head1.getCurrBranch().equals(a)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        toDelete.delete();
    }

    public static void merge(String a) {
        boolean isConflict = false;
        if (!Utils.plainFilenamesIn(ADD_FOLDER).isEmpty() || !Utils.plainFilenamesIn(RM_FOLDER).isEmpty()) {
            System.out.println("You have uncommited changes.");
            System.exit(0);
        }
        File branch = new File(".gitlet/branches/" + a);
        if (!branch.exists()){
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        Pointer givBranch = Pointer.writePointer(a); //given branch
        Pointer head1 = Pointer.writePointer("head");
        if (a.equals(head1.getCurrBranch())) {
            System.out.println("Cannot merge a branch with itself");
            System.exit(0);
        }
        Pointer currBranch = Pointer.writePointer(head1.getCurrBranch()); //current branch
        String currSha = currBranch.getPointsTo(); //current branch's commit's sha1
        String givenSha = givBranch.getPointsTo();//given branch's commit's sha1
        String givenShaHold = givBranch.getPointsTo();
        String splitPointSha = null; //split point's commit's sha1
        int i = 0;
        while (currSha != null) {
            Commit c = Commit.writeCommit(currSha);
            givenSha = givenShaHold;
            while (givenSha != null) {
                Commit g = Commit.writeCommit(givenSha);
                if (c.getSha().equals(g.getSha())) {
                    splitPointSha = c.getSha();
                    i += 1;
                    break;
                }
                if (c.getParentSha().equals(g.getParentSha())) {
                    splitPointSha = c.getParentSha();
                    i += 1;
                    break;
                }
                else if (c.getParentSha2() != null && c.getParentSha2().equals(g.getParentSha())) {
                    splitPointSha = g.getParentSha();
                    i += 1;
                    break;
                }
                else if (g.getParentSha2() != null && g.getParentSha2().equals(c.getParentSha())) {
                    splitPointSha = c.getParentSha();
                    i += 1;
                    break;
                }

                givenSha = g.getParentSha();
            }
            if (i > 0) {
                break;
            }
            currSha = c.getParentSha();
        }
        if (splitPointSha.equals(givBranch.getPointsTo())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (splitPointSha.equals(head1.getPointsTo())) {
            String[] checkoutArgs = {"branch", a};
            checkout(checkoutArgs);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
        Commit split = Commit.writeCommit(splitPointSha);
        HashMap<String, String> splitFiles = split.getFiles();
        Commit c = Commit.writeCommit(head1.getPointsTo());
        Commit g = Commit.writeCommit(givenSha);
        HashMap<String, String> currFiles = c.getFiles(); // names as keys, sha1s of blobs as values
        HashMap<String, String> givFiles = g.getFiles();
        File curD = new File ("./");
        List<String> wdFiles = Utils.plainFilenamesIn(curD);
        // If an untracked file in the current commit would be overwritten or deleted by the merge:

        for (String s : wdFiles) {
            if (!currFiles.containsKey(s)) { //if this is a new/updated blob
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        //1. any files that've been modified in the given branch since split point, but not modified in current branch
        //since split point are changed to their version in given branch (checked out from the commit at front of giv branch
        //these files will be automatically staged

        //files with same name between currFiles, splitFiles, and givFiles,
        // and same val between currFiles and splitFiles but not givFiles
        for (String name : givFiles.keySet()) {
            String gFileSha = givFiles.get(name);
            if (splitFiles != null && splitFiles.containsKey(name) && currFiles.containsKey(name)) { //name: giv, split, curr
                String sFileSha = splitFiles.get(name);
                String cFileSha = currFiles.get(name);
                if (!gFileSha.equals(sFileSha) && cFileSha.equals(sFileSha)) { //1: name: giv, split, curr. s = c, s != g
                    checkout(new String[]{"checkout", givBranch.getPointsTo(), "--", name});
                    add(name);
                } else if (!cFileSha.equals(sFileSha) && gFileSha.equals(sFileSha)) { //2: name: giv, split, curr. s = g, s != c
                    continue;
                } else if (cFileSha.equals(gFileSha)) { //3: name: giv, split, curr. s = g = c or s ! = c = g
                    continue;
                } else if (!cFileSha.equals(sFileSha) && !cFileSha.equals(gFileSha) && !gFileSha.equals(sFileSha)) { //8a name: giv, split, curr. s != c != g
                    isConflict = true;
                    mergeConflict(name, gFileSha, cFileSha);
                }
            } else if ((splitFiles == null || !splitFiles.containsKey(name)) && !currFiles.containsKey(name)) { //5: name: giv. no name: split, curr
                checkout(new String[]{"checkout", givBranch.getPointsTo(), "--", name});
                add(name);
            } else if ((splitFiles == null || !splitFiles.containsKey(name)) && !currFiles.get(name).equals(gFileSha)) { //8d: name: giv, curr. no name: split. c!= g
                String cFileSha = currFiles.get(name);
                isConflict = true;
                mergeConflict(name, gFileSha, cFileSha);
            } else if (splitFiles != null && !currFiles.containsKey(name) && !splitFiles.get(name).equals(gFileSha)) { //8c: name: giv, split. no name: curr. s!= g.
                isConflict = true;
                mergeConflict(name, gFileSha, null);
            } else if (splitFiles != null && !currFiles.containsKey(name) && splitFiles.get(name).equals(gFileSha)) { //7: name: split, giv. no name: curr. s = g.
                continue;
            }
        }
        for (String name : currFiles.keySet()) {
            String cFileSha = currFiles.get(name);
            if ((splitFiles == null || !splitFiles.containsKey(name)) && !givFiles.containsKey(name)) { //4: name: curr. no name: split, giv.
                continue;
            }
            else if (splitFiles != null && !givFiles.containsKey(name) && splitFiles.get(name).equals(cFileSha)) { //6: name: curr, split. no name: giv. s = c
                rm(name);
            } else if (splitFiles != null && !givFiles.containsKey(name) && !splitFiles.get(name).equals(cFileSha)) { //8b: name: curr, split. no name: giv. s != c
                String sFileSha = splitFiles.get(name);
                isConflict = true;
                mergeConflict(name, null, cFileSha);
            }
        }
        mergeCommit("Merged " + a + " into " + currBranch.getName() + ".", a);
        if (isConflict) {
            System.out.println("Encountered a merge conflict.");
        }

        //2. any files modified in curr branch but not modified in given branch since split point stay as they are
        //3. any files modified in both curr and given branch in same way are left unchanged
        //4. any files not at split point and present only in given branch should be checked out and staged
        //5. any files present at split point, unmodified in curr branch, and absent in given branch should be removed
        //& untracked
        //6. any files at split point, unmodified in giv branch, & absent in curr branch, should remain absent
        //7. any files modified in DIFF WAYS in curr & giv branch are IN CONFLICT
    }


    public static void mergeCommit(String message, String branch) {
        Pointer head1 = Pointer.writePointer("head");
        Pointer givenBranch = Pointer.writePointer(branch);
        Pointer currBranch = Pointer.writePointer(head1.getCurrBranch());
        String parentSha = head1.getPointsTo(); //sha1 id of parent commit
        String parentSha2 = givenBranch.getPointsTo();
        Commit parent = Commit.writeCommit(parentSha);
        HashMap<String, String> hashFile = new HashMap<>();
        if (parent.getFiles() != null) {
            hashFile.putAll(parent.getFiles());
        }
        if (!Utils.plainFilenamesIn(RM_FOLDER).isEmpty()) { // if remove folder has things in it
            List<String> rmNames = Utils.plainFilenamesIn(RM_FOLDER);
            for (String r : rmNames) {
                hashFile.remove(r);
                File rmFile = new File(".gitlet/staging/rmFolder/" + r);
                rmFile.delete();
            }
            if (Utils.plainFilenamesIn(ADD_FOLDER).isEmpty()){ // if nothing in add folder
                Commit newCommit = new Commit(message, parentSha, hashFile);
                String sha = Utils.sha1(Utils.serialize(newCommit));
                newCommit.setSha(sha);
                newCommit.setWasMerged();
                newCommit.setParentSha2(parentSha2);
                newCommit.saveCommit();
                head1.update(newCommit.getSha(), head1.getCurrBranch());
                head1.savePointer();
                currBranch.update(newCommit.getSha(), null);
                currBranch.savePointer();
            }
        }
        if (!Utils.plainFilenamesIn(ADD_FOLDER).isEmpty()) { // if add folder has things in it
            List<String> stagingNames = Utils.plainFilenamesIn(ADD_FOLDER); //list of names of files in add folder
            // adds names as keys and sha of blob as values into hashFile from staging add area, removes them from staging area
            for (String i : stagingNames) {
                File I_FILE = new File(".gitlet/staging/addFolder/" + i); // pointer to file in staging add area
                String blobSha = Utils.readContentsAsString(I_FILE);
                hashFile.put(I_FILE.getName(), blobSha); // adds name as key, sha as val to hashFile
                I_FILE.delete();
            }
            Commit newCommit = new Commit(message, parentSha, hashFile);
            String sha = Utils.sha1(Utils.serialize(newCommit));
            newCommit.setSha(sha);
            newCommit.setParentSha2(parentSha2);
            newCommit.setWasMerged();
            newCommit.saveCommit(); //saves new commit to a file named by sha1 id containing commit object in commits folder
            head1.update(newCommit.getSha(), head1.getCurrBranch());
            head1.savePointer();//updates head pointer
            currBranch.update(newCommit.getSha(), null);
            currBranch.savePointer();//updates branch pointer
        }
    }

    public static void mergeConflict(String fileName, String givenID, String currID) {
        File wdFile = new File(fileName);
        if (givenID != null && currID == null) {
            File givenFile = new File(".gitlet/blobs/" + givenID);
            String givenContents = Utils.readContentsAsString(givenFile);
            Utils.writeContents(wdFile, "<<<<<<< HEAD" + System.lineSeparator() + "======="
                    + givenContents + ">>>>>>>"
                    + System.lineSeparator());
            add(fileName);
        }
        else if (currID != null && givenID == null) {
            File currentFile = new File(".gitlet/blobs/" + currID);
            String currentContents = Utils.readContentsAsString(currentFile);
            Utils.writeContents(wdFile, "<<<<<<< HEAD" + System.lineSeparator()
                    + currentContents + "=======" + System.lineSeparator()
                    + ">>>>>>>" + System.lineSeparator());
            add(fileName);
        }
        else {
            File givenFile = new File(".gitlet/blobs/" + givenID);
            String givenContents = Utils.readContentsAsString(givenFile);
            File currentFile = new File(".gitlet/blobs/" + currID);
            String currentContents = Utils.readContentsAsString(currentFile);
            Utils.writeContents(wdFile, "<<<<<<< HEAD" + System.lineSeparator()
                    + currentContents + "======="
                    + System.lineSeparator() + givenContents + ">>>>>>>" + System.lineSeparator());
            add(fileName);
        }
    }
>>>>>>> 2abddd335d450ccc305be0cbfe717cb43b9cb205
}

