package cpw.mods.forge.cursepacklocator;

public interface IProgressOutput {

    void displayAndWait();

    void finish();

    void setFileCount(int size);

    void beginFile(String projectID, String fileID, String fileName);

    void endFile(String projectID, String fileID);
}
