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
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.ResolveMerger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.FileTreeIterator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
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

                if ( item.endsWith(".dbs")) {
                    final Button mergeButton = new Button(rx.getText("sync"));
                    mergeButton.setOnAction(ev -> syncModelWithOrigin( item ));
                    box.getChildren().add(mergeButton);
                }

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
                new FxGitSynchronizationDialog( fxGitExplorer, "Origin", originProject, "Local", fxGitExplorer.getLocalFileContent() ).
                        showDialog().ifPresent( result -> {
                            try {
                                Files.writeString( fxGitExplorer.getGitFile().toPath(), result, StandardOpenOption.WRITE );
                            } catch ( IOException err){
                                rx.showError( getDialogScene(), err );
                            }
                        });
            }
            git.add().addFilepattern( filePath ).call();
        } catch (Exception ex ){
            rx.showError( getDialogScene(), ex );
        }
    }

    private void acceptMine(String filePath ){
        try {
            git.fetch().setRemote("origin").setCredentialsProvider( git.getCredentialsProvider( getDialogScene() )).call();
            git.authenticationSucceeded();
            git.add().addFilepattern( filePath).call();
            git.commit().setMessage("Conflict solved - accept mine.").setAll(false).setOnly(filePath).call();
            rx.showInformation(getDialogScene(), "Done");
        } catch (Exception ex ){
            rx.showError( getDialogScene(), ex );
        }
    }


    private void acceptTheirs( String filePath ) {
        try {
            fxGitExplorer.installFileWatcher();
            final ResolveMerger merger = (ResolveMerger) MergeStrategy.THEIRS.newMerger(git.getRepository(), true);
            merger.setWorkingTreeIterator(new FileTreeIterator(git.getRepository()));
            final ObjectId headId = git.getRepository().resolve("HEAD");
            final ObjectId originHeadId = git.getRepository().resolve("origin/" + git.getRepository().getBranch() + "/" + filePath);
            merger.setBase(headId);
            if (merger.merge(originHeadId, headId)) {
                //TODO: Should I check also merger.getUnmergedPaths() ?
                if ( merger.getFailingPaths()!= null && !merger.getFailingPaths().isEmpty() ){
                    new FxGitConflictsDialog(fxGitExplorer, git, new ArrayList<>(merger.getFailingPaths().keySet()) ).showDialog();
                }
            }
            git.add().addFilepattern( filePath).call();
            fxGitExplorer.uninstallFileWatcher();
        } catch ( Exception ex ){
            rx.showError( getDialogScene(), ex );
        }
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