package cpw.mods.forge.cursepacklocator;

public class ServerProgressOutput implements IProgressOutput {

    // server has logging that is expected to be visible, so either no-op or display swing GUI if not headless

    @Override public void displayAndWait() {
    }

    @Override public void finish() {
    }

    @Override public void setFileCount(int size) {
    }

    @Override public void beginFile(String projectID, String fileID, String fileName) {
    }

    @Override public void endFile(String projectID, String fileID) {
    }
}
