package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static gitlet.Repository.ADD_STAGE;
import static gitlet.Repository.REMOVE_STAGE;

public class Stage implements Serializable {
    public static final File ADD_STAGE_FILE = Utils.join(Repository.GITLET_DIR, "add_stage");
    private Map<String, String> addStage;      // 文件名 → blobID
    private Set<String> removeStage;           // 文件名集合
    public Stage() {
        addStage = new HashMap<>();
        removeStage = new HashSet<>();
    }
    public void addFileToStage(String fileName, String blobID){
        // 将文件添加到添加暂存区
        addStage.put(fileName,blobID);
        // 如果该文件曾被标记为删除，则取消删除
        removeStage.remove(fileName);
    }
    //从添加暂存区撤销文件的添加
    public void deleteAddstage(String fileName){
        addStage.remove(fileName);
    }

    public Map<String, String> getAddStage() {
        return addStage;
    }


    public Set<String> getRemoveStage() {
        return removeStage;
    }
    public void save(){
        // 分别保存add_stage和remove_stage
        Utils.writeObject(ADD_STAGE, (Serializable) addStage);
        Utils.writeObject(REMOVE_STAGE, (Serializable) removeStage);
    }
    public boolean IsStageEmpty(){
        if(addStage.isEmpty()&&removeStage.isEmpty()){
            return true;
        }
        else{
            return false;
        }
    }
    public void clear() {
        addStage.clear();
        removeStage.clear();
    }
    //返回文件是否在添加暂存区
    public boolean addStageFileExists(String fileName) {
        return addStage.containsKey(fileName);
    }
    public void addFileToRemoveStage(String fileName){
        removeStage.add(fileName);
        addStage.remove(fileName);  // 如果它曾被 add，则取消 add

    }
    //加载 stage 对象
    public static Stage loadStage() {
        Stage stage = new Stage();
        if (ADD_STAGE.exists()) {
            stage.addStage = Utils.readObject(ADD_STAGE, HashMap.class);
        }
        if (REMOVE_STAGE.exists()) {
            stage.removeStage = Utils.readObject(REMOVE_STAGE, HashSet.class);
        }
        return stage;
    }
}
