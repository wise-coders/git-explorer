package com.wisecoders;

import com.wisecoders.util.Action;
import com.wisecoders.util.Dialog$;
import com.wisecoders.util.StringUtil;
import com.wisecoders.util.VBox$;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.transport.PushResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class FxGitCommitDialog extends Dialog$ {


    public static final int CONFLICTING = 1;
    public static final int ADDED = 2;
    public static final int MISSING = 4;
    public static final int MODIFIED = 8;
    public static final int REMOVED = 16;
    public static final int UNCOMMITTED = 32;
    public static final int UNTRACKED = 64;

    private final GitTreeItem root = new GitTreeItem("Root");

    protected final TreeView<GitFile> treeView = new TreeView<>();
    private final TextArea messageTextArea = new TextArea();
    private final XGit git;

    private boolean hasUncommittedChanges = false;
    private List<String> conflicts = new ArrayList<>();

    private final FxGitExplorer fxGitExplorer;
    public FxGitCommitDialog(FxGitExplorer fxGitExplorer, XGit git) {
        super(fxGitExplorer);
        this.fxGitExplorer = fxGitExplorer;
        this.git = git;
        this.rx.addFlag("flagCanCommit", ()-> hasSelectedNodes() && StringUtil.isFilledTrim(messageTextArea.getText()) );


        treeView.setRoot( root );
        treeView.setShowRoot( false );
        treeView.setCellFactory( prop -> new FxGitTreeCell() );
        messageTextArea.textProperty().addListener((o,p,c)-> rx.fireEvents());

        try {
            Status status = git.status().setIgnoreSubmodules(SubmoduleWalk.IgnoreSubmoduleMode.NONE).call();
            final GitTreeItem conflictsItem = root.createFolder("Conflicts" );
            conflictsItem.createChildren( status.getConflicting(), CONFLICTING );
            final GitTreeItem changesItem = root.createFolder("Changelist" );
            changesItem.createChildren( status.getAdded(), ADDED );
            changesItem.createChildren( status.getMissing(), MISSING );
            changesItem.createChildren( status.getModified(), MODIFIED );
            changesItem.createChildren( status.getRemoved(), REMOVED );
            changesItem.createChildren( status.getUncommittedChanges(), UNCOMMITTED );
            if ( status.getUncommittedChanges() != null && !status.getUncommittedChanges().isEmpty()){
                hasUncommittedChanges = true;
            }
            if ( status.getConflicting() != null && !status.getConflicting().isEmpty()){
                this.conflicts.addAll( status.getConflicting() );
            }

            final GitTreeItem untrackedItem = root.createFolder("Untracked" );
            untrackedItem.createChildren( status.getUntracked(), UNTRACKED );
            //root.createChild("Untracked Folders", status.getUntrackedFolders());
            root.clearEmptyFolders();

        } catch ( Throwable ex ){
            rx.showError( getDialogScene(), ex );
        }
    }

    public boolean hasUncommittedChanges(){
        return hasUncommittedChanges;
    }
    public boolean hasConflictingChanges(){
        return !conflicts.isEmpty();
    }

    public List<String> getConflicts(){
        return conflicts;
    }


    @Override
    public Node createContentPane() {
        messageTextArea.setPrefSize( 300, 150 );
        VBox.setVgrow( treeView, Priority.ALWAYS );
        VBox.setVgrow( messageTextArea, Priority.ALWAYS );
        return new VBox$().withGap(5).withChildren(
                treeView,
                rx.label("messageLabel"),
                messageTextArea );
    }

    @Override
    public void createButtons() {
        createActionButton("gitCommit");
        createActionButton("gitCommitAndPush");
        createActionButton("gitPush");
        createCancelButton();
    }

    @Action(enabledProperty = "flagCanCommit")
    public Task gitCommit(){
        return createCommitTask( true, false );
    }
    @Action(enabledProperty = "flagCanCommit")
    public Task gitCommitAndPush(){
        return createCommitTask( true, true );
    }
    @Action
    public Task gitPush(){
        return createCommitTask( false, true );
    }

    private Task createCommitTask( boolean commit, boolean push ){
        return new Task<Iterable<PushResult>>() {
            @Override
            protected Iterable<PushResult> call() throws Exception {

                updateMessage("Check conflicts...");
                git.fetch().setRemote("origin").setCredentialsProvider( git.getCredentialsProvider( getDialogScene()) ).call();
                git.authenticationSucceeded();

                final ArrayList<String> toAdd = new ArrayList<>();
                for ( TreeItem<GitFile> folder : root.getChildren()) {
                    for ( TreeItem<GitFile> item : folder.getChildren() ){
                        if ( item.getValue().isSelected() ) {
                            toAdd.add( item.getValue().fileName );
                        }
                    }
                }

                if ( commit ) {
                    updateMessage("Do commit...");
                    if (toAdd.isEmpty()) {
                        throw new IOException("Nothing to commit");
                    }
                    git.reset().call();
                    final AddCommand addCommand = git.add();
                    for (String filePattern : toAdd) {
                        addCommand.addFilepattern(filePattern);
                    }
                    addCommand.call();
                    git.commit()
                            .setMessage(messageTextArea.getText())
                            .setCredentialsProvider(git.getCredentialsProvider( getDialogScene()))
                            .call();
                }
                if ( push ){
                    updateMessage("Push changes...");
                    return git.push().setRemote("origin").setCredentialsProvider(git.getCredentialsProvider( getDialogScene() )).call();
                }
                return null;
            }

            @Override
            protected void succeeded() {
                if ( push ){
                    git.authenticationSucceeded();
                }
                final StringBuilder sb = new StringBuilder("Done\n");
                if ( getValue() != null ) {
                    for (PushResult result : getValue()) {
                        sb.append(result.getMessages());
                    }
                }
                rx.showInformation( getDialogScene(), sb.toString() );
                FxGitCommitDialog.this.close();
            }

            @Override
            protected void failed() {
                rx.showError(getDialogScene(), getException());

            }
        };
    }


    private boolean hasSelectedNodes() {
        for (TreeItem<GitFile> folder : root.getChildren()) {
            for (TreeItem<GitFile> item : folder.getChildren()) {
                if ( item.getValue().isSelected() ) return true;
            }
        }
        return false;
    }
    @Override
    public boolean apply() {
        if ( StringUtil.isEmpty( messageTextArea.getText() ) ) {
            rx.showError(getDialogScene(), "Please set a commit message");
            return false;
        }
        if ( !hasSelectedNodes()) {
            rx.showError(getDialogScene(), "Please select the files to commit.");
            return false;
        }
        return true;
    }


    public static class GitFile{

        public String fileName;
        private int flags = 0;
        private final SimpleBooleanProperty selectedProperty = new SimpleBooleanProperty( false );

        public GitFile( String fileName ){
            this.fileName = fileName;
        }
        public void addFlags(int flags ){
            this.flags |= flags;
        }
        public int getFlags(){
            return flags;
        }
        public void setSelected( boolean selected ){
            this.selectedProperty.set( selected );
        }
        public boolean isSelected(){
            return selectedProperty.get();
        }
        public SimpleBooleanProperty getSelectedProperty(){ return selectedProperty; }

        public String getStyleClass(){
            if ( (flags & UNTRACKED) > 0 ) return "text-red";
            if ( (flags & CONFLICTING) > 0 ) return "text-red";
            if ( (flags & ADDED) > 0 ) return "text-green";
            if ( (flags & MISSING) > 0 ) return "text-red";
            if ( (flags & MODIFIED) > 0 ) return "text-blue";
            if ( (flags & REMOVED) > 0 ) return "text-red";
            return null;
        }
    }

    private static class GitTreeItem extends TreeItem<GitFile>{

        public GitTreeItem( String name ){
            super( new GitFile( name ));
            setExpanded( true );
        }
        public GitTreeItem createFolder( String name ){
            GitTreeItem item = new GitTreeItem(name);
            getChildren().add( item );
            if ( getParent() != null ) {
                getChildren().sort(Comparator.comparing(o -> o.getValue().fileName));
            }
            return item;
        }
        public GitTreeItem createChild( String name, int flags ){
            GitTreeItem item = null;
            for ( TreeItem<GitFile> other : getChildren() ) {
                if (other instanceof GitTreeItem) {
                    GitTreeItem otherItem = (GitTreeItem) other;
                    if (otherItem.getValue().fileName.equals(name)) {
                        item = otherItem;
                    }
                }
            }
            if ( item == null ) {
                item = new GitTreeItem(name);
                getChildren().add( item );
                if ( getParent() != null ) {
                    getChildren().sort(Comparator.comparing(o -> o.getValue().fileName));
                }
            }
            item.getValue().addFlags( flags );
            return item;
        }

        public void createChildren(Set<String> names, int flags ){
            if ( names != null ){
                for ( String name : names ) {
                    createChild( name, flags );
                }
            }
        }

        public void clearEmptyFolders(){
            List<TreeItem<GitFile>> toRemove = new ArrayList<>();
            for ( TreeItem<GitFile> other :getChildren()){
                if ( other.getChildren().isEmpty()) toRemove.add(other );
            }
            getChildren().removeAll( toRemove);
        }
    }

    public class FxGitTreeCell extends TreeCell<GitFile> {


        @Override
        protected void updateItem(GitFile item, boolean empty) {
            super.updateItem(item, empty);
            setText( null );
            setGraphic( null );
            if ( item != null && !empty ){
                final CheckBox checkBox = new CheckBox( item.fileName );
                checkBox.setSelected( item.isSelected() );
                checkBox.selectedProperty().bindBidirectional( item.getSelectedProperty() );
                checkBox.selectedProperty().addListener((o,p,c)-> {
                    if ( getTreeItem() != null ) {
                        for (TreeItem<GitFile> child : getTreeItem().getChildren()) {
                            child.getValue().setSelected(c);
                        }
                    }
                    rx.fireEvents();
                });
                String styleClass = item.getStyleClass();
                if ( styleClass != null ) {
                    checkBox.getStyleClass().add( styleClass );
                }
                setGraphic( checkBox );
            }

        }
    }

}