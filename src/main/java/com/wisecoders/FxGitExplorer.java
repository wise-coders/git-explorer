package com.wisecoders;

import com.wisecoders.util.*;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.merge.ResolveMerger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.submodule.SubmoduleWalk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;

public class FxGitExplorer extends Scene {

    private final Rx rx = new Rx(  FxGitExplorer.class, this );
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d-MMM-yyyy HH:mm:ss");

    private final Stage stage;
    private final TableView<RevCommit> tableView = new TableView<>();
    private XGit git;
    private final List<RevCommit> allItems = new ArrayList<>();
    private final ObservableList<RevCommit> filteredItems = FXCollections.observableArrayList();
    private final TextField filterField = new TextField();
    private final SplitMenuButton branchButton = new SplitMenuButton();
    private boolean showAllRevisions = false;

    private WatchService fileChangesWatcher;
    protected WatchKey fileChangesWatcherKey;
    private File gitFile;
    private String gitFileInternalPath;


    public FxGitExplorer(Stage stage, BorderPane root ) {
        super( root );
        this.stage = stage;
        rx.addFlag("flagSelectedOne", () -> tableView.getSelectionModel().getSelectedItems().size() == 1);
        rx.addFlag("flagSelectedTwo", () -> tableView.getSelectionModel().getSelectedItems().size() == 2);
        rx.addFlag("flagSelectedMany", () -> tableView.getSelectionModel().getSelectedItems().size() > 1 );
        rx.addFlag("flagIsShowAllRevisions", this::isShowAllRevisions);
        rx.addFlag("flagHasRepository", () -> git != null);

        root.setCenter( createContentPane() );

        final TableColumn<RevCommit, String> nameColumn = new TableColumn<>("Revision");
        nameColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getName()));

        final TableColumn<RevCommit, String> authorColumn = new TableColumn<>("Author");
        authorColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getAuthorIdent().getName()));

        final TableColumn<RevCommit, String> timeColumn = new TableColumn<>("Time");
        timeColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper(DATE_FORMAT.format(param.getValue().getAuthorIdent().getWhen())));

        final TableColumn<RevCommit, String> commentColumn = new TableColumn<>("Comment");
        commentColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getFullMessage()));

        tableView.getColumns().addAll(nameColumn, timeColumn,authorColumn,  commentColumn );
        tableView.setPrefSize( 1100, 600);
        Rx.setTableViewColumnWidths(tableView, .2, .15, .1, .55);

        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener) c  -> rx.fireEvents());
        tableView.setItems( filteredItems );

        filterField.getStyleClass().add( "search-field");
        filterField.textProperty().addListener((o,p,c)-> applyFilter() );

        stage.setOnHiding(ev -> uninstallFileWatcher() );

        final Timeline updateTimer = new Timeline(new KeyFrame(Duration.seconds(0.1), ae -> processWatchEvents()));
        updateTimer.setCycleCount(Animation.INDEFINITE);
        updateTimer.play();

        new FxGitFileChooser(this).showDialog().ifPresent(val-> {
            gitFile = val;
            rx.executeTask( createReloadRevisionsTask() );
        });

    }




    public Node createContentPane() {


        final SplitMenuButton reloadRevisionsButton = rx.createSplitMenuButton("createReloadRevisionsTask", true );
        reloadRevisionsButton.getItems().add( rx.checkMenuItem("showAllRevisions"));
        reloadRevisionsButton.getItems().addAll( rx.menuItems( Rx.SEPARATOR, "createReloadRevisionsTask"));


        final HBox$ toolBar = new HBox$().withSmallGap().withChildren(
                rx.button("gitClone"),
                rx.button("gitPull"),
                rx.button("gitCommit"),
                new HGrowBox$(),
                branchButton,
                reloadRevisionsButton );

        final HBox$ toolBar2 = new HBox$().withSmallGap().withChildren(
                rx.button("downloadModelFile"),
                rx.button("diffCurrentModel"),
                rx.button("diffSelected"),
                rx.button("audit")
        );

        return new VBox$().withGap(5).withChildren(toolBar, filterField, tableView, toolBar2 );
    }

    @Action
    public void gitClone() {
        new FxGitCloneDialog( getWindow()).showDialog();
    }

    @Action(enabledProperty = "flagHasRepository")
    public Task gitPull() {
        try {
            git.fetch().setRemote("origin").setCredentialsProvider( git.getCredentialsProvider( getDialogScene()) ).call();
            final Status status = git.status().setIgnoreSubmodules(SubmoduleWalk.IgnoreSubmoduleMode.NONE).call();
            if ( status.getUncommittedChanges() != null && !status.getUncommittedChanges().isEmpty()) {
                final FxGitCommitDialog commitDialog = new FxGitCommitDialog( this, git );
                commitDialog.setHeaderText(rx.getText("repositoryHasUncommittedChanges"));
                commitDialog.showDialog();
                return createReloadRevisionsTask();
            } else if ( status.getConflicting() != null && !status.getConflicting().isEmpty() ) {
                new FxGitConflictsDialog(this, git, new ArrayList<>(status.getConflicting()));
                return createReloadRevisionsTask();
            }
        } catch (GitAPIException ex ){
            rx.showError( getDialogScene(), ex );
        }

        final FxGitCommitDialog commitDialog = new FxGitCommitDialog( this, git );
        if ( commitDialog.hasUncommittedChanges() ) {
            commitDialog.setHeaderText(rx.getText("repositoryHasUncommittedChanges"));
            commitDialog.showDialog();
            return null;
        } else if ( commitDialog.hasConflictingChanges() ){
            new FxGitConflictsDialog( this, git, commitDialog.getConflicts() );
            return null;

        } else {
            return new Task<PullResult>() {
                @Override
                protected PullResult call() throws Exception {
                    updateMessage("Running GIT pull...");
                    installFileWatcher();
                    return git.pull().
                            setRemote("origin").
                            setRemoteBranchName(git.getRepository().getBranch()).
                            setCredentialsProvider(git.getCredentialsProvider(getDialogScene())).
                            call();
                }

                @Override
                protected void succeeded() {
                    git.authenticationSucceeded();
                    uninstallFileWatcher();
                    final MergeResult mergeResult = getValue().getMergeResult();
                    if (mergeResult != null) {
                        if (mergeResult.getConflicts() != null && !mergeResult.getConflicts().isEmpty()) {
                            new FxGitConflictsDialog(FxGitExplorer.this, git, new ArrayList<>(mergeResult.getConflicts().keySet())).showDialog();
                        } else if (mergeResult.getMergeStatus().isSuccessful()) {
                            rx.showInformation(FxGitExplorer.this, mergeResult.toString());
                        } else {
                            // throw error for uncommitted changes.
                            final StringBuilder sb = new StringBuilder();
                            // This can happen if there are uncommitted changes and the pull runs
                            for (Map.Entry<String, ResolveMerger.MergeFailureReason> entry : mergeResult.getFailingPaths().entrySet()) {
                                String reason = entry.getValue().toString();
                                if (reason.contains("DIRTY_WORKTREE")) {
                                    reason = "has un-committed changes. " + reason;
                                }
                                sb.append(entry.getKey()).append(" ").append(reason).append("\n\n");
                            }
                            sb.append(mergeResult);
                            rx.showError(FxGitExplorer.this, sb.toString());
                        }
                    }
                }

                @Override
                protected void failed() {
                    uninstallFileWatcher();
                    if (getException() instanceof CheckoutConflictException) {
                        CheckoutConflictException ex = (CheckoutConflictException) getException();
                        new FxGitConflictsDialog(FxGitExplorer.this, git, ex.getConflictingPaths()).showDialog();
                    } else {
                        String message = getException().getLocalizedMessage();
                        if (message.contains("DIRTY_WORKTREE")) {
                            message = "Your repository contains un-committed changes.\n" + message;
                        }
                        rx.showError(FxGitExplorer.this, message, getException());
                    }
                }
            };
        }
    }

    @Action(enabledProperty = "flagHasRepository")
    public Task gitCommit() {
        new FxGitCommitDialog(this, git).showDialog();
        return createReloadRevisionsTask();
    }


    @Action(enabledProperty = "flagSelectedOne")
    public void diffCurrentModel() {
        try {
            final String gitFileContent = git.loadProjectFromRevision(gitFileInternalPath, tableView.getSelectionModel().getSelectedItem());
            new FxGitSynchronizationDialog(this, "GIT Server", gitFileContent, "Local", getLocalFileContent() ).showDialog().ifPresent(
                    result -> {
                        try {
                            Files.writeString( gitFile.toPath(), result, StandardOpenOption.WRITE );
                        } catch (IOException ex ){
                            rx.showError( getDialogScene(), ex );
                        }
                    }
            );

        } catch (Exception ex) {
            rx.showError(getDialogScene(), ex);
        }
    }

    public String getLocalFileContent() throws IOException {
        return Files.readString(gitFile.toPath());
    }

    @Action(enabledProperty = "flagSelectedOne")
    public void downloadModelFile() {
        try {
            final File file = new FileChooser().showSaveDialog(getDialogScene().getWindow());
            if (file != null) {
                try (FileOutputStream writer = new FileOutputStream( file )) {
                    git.copyRevisionFileToOutputStream(gitFileInternalPath, tableView.getSelectionModel().getSelectedItem(), writer );
                }
            }
        } catch (Exception ex) {
            rx.showError(getDialogScene(), ex);
        }
    }

    @Action(enabledProperty = "flagSelectedTwo")
    public void diffSelected() {
        try {
            List<RevCommit> selectedCommits = tableView.getSelectionModel().getSelectedItems();
            if ( selectedCommits.size() == 2 ) {
                final String text1 = git.loadProjectFromRevision(gitFileInternalPath, selectedCommits.get(0));
                final String text2 = git.loadProjectFromRevision(gitFileInternalPath, selectedCommits.get(1));
                new FxGitSynchronizationDialog(this, selectedCommits.get(0).getName(), text1, selectedCommits.get(1).getName(), text2 ).showDialog();
            }
        } catch (Exception ex) {
            rx.showError(getDialogScene(), ex);
        }
    }

    @Action(enabledProperty = "flagSelectedMany")
    public void audit() {
    }


    public boolean isShowAllRevisions(){
        return showAllRevisions;
    }
    @Action( selectedProperty = "flagIsShowAllRevisions")
    public Task showAllRevisions() {
        this.showAllRevisions = !showAllRevisions;
        return createReloadRevisionsTask();
    }

    @Action
    public Task createReloadRevisionsTask() {
            stage.setTitle("GIT Dialog - " + gitFile);
        return new ListRevisionsTask();
    }


    private class ListRevisionsTask extends Task<List<RevCommit>> {

        @Override
        protected List<RevCommit> call() throws Exception {
            updateMessage("Loading revisions...");
            git = XGit.init(gitFile);
            gitFileInternalPath = git.getPath(gitFile);
            return git.log(gitFileInternalPath, isShowAllRevisions() );
        }

        @Override
        protected void succeeded() {
            allItems.clear();

            stage.setTitle( rx.getString("dialog.title") + " " + git.getRepository() );
            allItems.addAll(getValue());
            applyFilter();
            tableView.setDisable(false);

            try {
                branchButton.setText(git.getRepository().getBranch());
                branchButton.getItems().clear();
                branchButton.getItems().add(rx.menuItem("createBranch"));
                for (Ref ref : git.branchList().call()) {
                    final Menu branchMenu = new Menu(ref.getName());
                    branchButton.getItems().add(branchMenu);
                    if (ref.getName().equals(git.getRepository().getBranch())) {
                        branchButton.setText(ref.getName());
                    }
                    MenuItem checkoutItem = new MenuItem("Checkout");
                    checkoutItem.setOnAction((ev) -> checkout(ref.getName()));
                    MenuItem mergeItem = new MenuItem("Merge");
                    mergeItem.setOnAction((ev) -> merge(ref));
                    branchMenu.getItems().addAll(checkoutItem, mergeItem);
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
            rx.fireEvents();
        }

        @Override
        protected void failed() {
            tableView.setDisable(true);
            rx.fireEvents();
            rx.showError(getDialogScene(), "scanRepository", getException());
        }
    }


    @Action
    public Task createBranch(){
        final String branchName = rx.showInputString( getDialogScene(), "BranchName");
        if ( StringUtil.isFilled( branchName )) {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    git.branchCreate().setName( branchName ).setUpstreamMode( CreateBranchCommand.SetupUpstreamMode.TRACK).call();
                    return null;
                }

                @Override
                protected void failed() {
                    rx.showError(getDialogScene(), getException());
                }

                @Override
                protected void succeeded() {
                    try {
                        branchButton.setText(git.getRepository().getBranch());
                    } catch ( IOException ignore){}
                    rx.showInformation(FxGitExplorer.this, "Switched to " + branchName);
                }
            };
        }
        return null;
    }

    private Scene getDialogScene(){
        return this;
    }

    public void checkout( String branchName ){
        rx.executeTask( new Task<Void>(){
            @Override
            protected Void call() throws Exception {
                git.fetch().setRemote("origin").setCredentialsProvider( git.getCredentialsProvider( FxGitExplorer.this) ).call();
                git.authenticationSucceeded();

                git.checkout().setName( branchName ).setStartPoint("origin/" + branchName).call();
                return null;
            }

            @Override
            protected void succeeded() {
                try {
                    branchButton.setText(git.getRepository().getBranch());
                } catch (IOException ignore ){}
                rx.showInformation( FxGitExplorer.this, "Switched to " + branchName );
            }

            @Override
            protected void failed() {
                rx.showError(FxGitExplorer.this, getException());
            }

        } );
    }

    public void merge(Ref branchRef ){
        rx.executeTask( new Task<MergeResult>(){
            @Override
            protected MergeResult call() throws Exception {
                git.merge().include(branchRef).call();
                return null;
            }

            @Override
            protected void failed() {
                rx.showError( FxGitExplorer.this, getException() );
            }

            @Override
            protected void succeeded() {
                rx.showInformation( FxGitExplorer.this, String.valueOf( getValue().getMergeStatus()) );
            }
        } );
    }

    public void applyFilter() {
        filteredItems.setAll( allItems );
        String filter = ( filterField.getText() != null ? filterField.getText().toLowerCase() : null );
        if (StringUtil.isFilled( filter )){
            filteredItems.removeIf( unit -> !unitMatchesFiler( unit, filter ));
        }
        tableView.refresh();
    }

    private boolean unitMatchesFiler( RevCommit revCommit, String filter ){
        if ( filter == null ) return true;
        filter = filter.trim().toLowerCase();
        return revCommit.getAuthorIdent().getName().contains( filter )
                || revCommit.getAuthorIdent().getWhen().toString().contains( filter )
                || ( revCommit.getFullMessage() != null && revCommit.getFullMessage().contains( filter ));
    }

    public void installFileWatcher() throws IOException {
        if ( gitFile != null  ) {
            this.fileChangesWatcher = FileSystems.getDefault().newWatchService();
            this.fileChangesWatcherKey = gitFile.getParentFile().toPath().register(fileChangesWatcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        }
    }
    public void uninstallFileWatcher(){
        if ( fileChangesWatcher != null ) {
            Platform.runLater(() -> {
                try { fileChangesWatcher.close(); } catch (IOException ignore){}
                fileChangesWatcher = null;
                fileChangesWatcherKey = null;
            });
        }
    }

    private boolean showFileChangesReloadMessage = true;

    private void processWatchEvents() {
        if (fileChangesWatcherKey != null) {
            for (WatchEvent<?> watchEvent : fileChangesWatcherKey.pollEvents()) {
                WatchEvent.Kind<?> kind = watchEvent.kind();
                if (kind != OVERFLOW && watchEvent.context() instanceof Path) {
                    final Path filePath = (Path) watchEvent.context();
                    final Path folderPath = gitFile.getParentFile().toPath();

                    if (gitFile != null && gitFile.toPath().equals(folderPath.resolve(filePath)) && showFileChangesReloadMessage) {
                        showFileChangesReloadMessage = false;
                        Platform.runLater(() -> {
                            if (rx.showOptionsDialog(this, "reloadProjectFromFile").getDialogResult() == ButtonBar.ButtonData.YES) {
                                showFileChangesReloadMessage = true;
                            }
                        });
                    }
                }
            }
        }
    }

    public File getGitFile(){
        return gitFile;
    }

    public String getGitFileInternalPath(){
        return gitFileInternalPath;
    }
}
