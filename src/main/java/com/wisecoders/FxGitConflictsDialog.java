package com.wisecoders;

import com.wisecoders.util.Action;
import com.wisecoders.util.Dialog$;
import com.wisecoders.util.HBox$;
import com.wisecoders.util.HGrowBox$;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class FxGitConflictsDialog extends Dialog$ {

    private final XGit git;
    private final ListView<String> listView = new ListView<>();

    private final FxGitExplorer fxGitExplorer;

    public FxGitConflictsDialog(FxGitExplorer fxGitExplorer, XGit git, List<String> conflicts){
        super(fxGitExplorer);
        this.fxGitExplorer = fxGitExplorer;
        this.git = git;
        listView.getItems().addAll( conflicts );
    }

    @Override
    public Node createContentPane() {
        listView.setCellFactory( prop-> new ConflictCell());
        listView.setPrefSize( 600, 400);
        return listView;
    }

    @Override
    public void createButtons() {
        createOkButton();
        createCancelButton();
        createActionButton("commitAndPush");
    }

    @Override
    public boolean apply() {
        return true;
    }

    private class ConflictCell extends ListCell<String> {

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            setText( null );
            setGraphic( null );
            if ( item != null && !empty ){
                HBox$ box = new HBox$().
                        withAlignment(Pos.CENTER_LEFT).
                        withChildren(new Label(item), new HGrowBox$());
                setGraphic(box);


                final Button mergeButton = new Button(rx.getText("sync"));
                mergeButton.setOnAction(ev -> syncModelWithOrigin( item ));
                box.getChildren().add(mergeButton);


                final Button acceptYoursButton = new Button(rx.getText("acceptYours"));
                acceptYoursButton.setOnAction(ev -> acceptMine(item));
                box.getChildren().add(acceptYoursButton);

                final Button acceptTheirsButton = new Button(rx.getText("acceptTheirs"));
                acceptTheirsButton.setOnAction(ev -> acceptTheirs( item ) );
                box.getChildren().add(acceptTheirsButton);

            }
        }
    }


    private void syncModelWithOrigin(String filePath ){
        try {
            final RevCommit latestCommit = git.getRepository().parseCommit(git.getRepository().resolve("origin/" + git.getRepository().getBranch() ) );
            final String originProject = git.loadProjectFromRevision( filePath, latestCommit);

            if ( filePath.equals( git.getPath( fxGitExplorer.getGitFile()) )){
                // get the local changes to show on right side
                byte[] localFileContent = getDesiredFileContent("HEAD:" + filePath);
                String contentOfLocalFile = new String(localFileContent, StandardCharsets.UTF_8);

                new FxGitSynchronizationDialog( fxGitExplorer, "Origin", originProject, "Local", contentOfLocalFile ).
                        showDialog().ifPresent( result -> {
                            try {
                                Files.write(git.getRepository().getWorkTree().toPath().resolve(filePath), result.getBytes(StandardCharsets.UTF_8));
                            } catch ( IOException err){
                                rx.showError( getDialogScene(), err );
                            }
                        });
            }
            git.authenticationSucceeded();
            git.add().addFilepattern(filePath).call();
            git.commit().setMessage("Conflict resolved - Syncing mine and origin").call();
            rx.showInformation(getDialogScene(), "Done");
        } catch (Exception ex ){
            rx.showError( getDialogScene(), ex );
        }
    }

        private void acceptMine(String filePath ){
            try {
                //complete local content here
                byte[] localFileContent = getDesiredFileContent("HEAD:" + filePath);

                //writes the local content in file
                Files.write(git.getRepository().getWorkTree().toPath().resolve(filePath), localFileContent);

                git.authenticationSucceeded();
                git.add().addFilepattern(filePath).call();
                git.commit().setMessage("Conflict resolved - Accepted mine").call();
                rx.showInformation(getDialogScene(), "Done");
            } catch (Exception ex ){
                rx.showError( getDialogScene(), ex );
            }
        }


    private void acceptTheirs( String filePath ) {
        try {
            //complete origin content here
            byte[] originFileContent = getDesiredFileContent("origin/" + git.getRepository().getBranch() +":"  + filePath);

            //writes the origin content in file
            Files.write(git.getRepository().getWorkTree().toPath().resolve(filePath), originFileContent);

            git.authenticationSucceeded();
            git.add().addFilepattern(filePath).call();
            git.commit().setMessage("Conflict resolved - Accepted theirs").call();
            rx.showInformation(getDialogScene(), "Done");

        } catch ( Exception ex ){
            rx.showError( getDialogScene(), ex );
        }
    }

    private byte[] getDesiredFileContent(String completeFilePath) throws IOException {
        ObjectId originId = git.getRepository().resolve(completeFilePath);
        ObjectLoader originLoader = git.getRepository().open(originId);
        return originLoader.getBytes();
    }

    @Action
    public void commitAndPush() {
        new FxGitCommitDialog(fxGitExplorer, git).showDialog();
    }


}



/*
Old AcceptTheirs
final File file = new File(git.getRepository().getDirectory().getParentFile(), filePath);
try (FileOutputStream fos = new FileOutputStream(file)){
    git.copyRevisionFileToOutputStream( filePath, git.getRepository().parseCommit(git.getRepository().resolve("origin/" + git.getRepository().getBranch() )), fos );
    addAndCommitMergedFile( filePath );
} catch (Exception ex ){
    rx.showError( getDialogScene(), ex );
}
 */