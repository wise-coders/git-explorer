package com.wisecoders;

import javafx.scene.Scene;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class XGit extends Git {

    private final FxGitCredentialsProvider credentialsProvider = new FxGitCredentialsProvider();

    public final File gitFile;

    public XGit(Repository repo, File gitFile) {
        super(repo);
        this.gitFile = gitFile;
    }

    public ObjectId getOriginBranchRef() throws Exception{
        return getRepository().resolve("HEAD");
    }

    public FxGitCredentialsProvider getCredentialsProvider( Scene scene ){
        credentialsProvider.setScene( scene );
        return credentialsProvider;
    }

    public void authenticationSucceeded(){
        credentialsProvider.usedSuccessfully();
    }

    public static XGit init(File gitFile ) throws IOException {

        if ( gitFile == null || !gitFile.exists()) {
            throw new IOException("Please save the design model to file.");
        }

        final RepositoryBuilder builder = new RepositoryBuilder()
                .readEnvironment()
                .findGitDir(gitFile);
        if ( builder.getGitDir() != null ) {
            Repository repo = builder.build();
            return new XGit(repo, gitFile );
        }
        throw new IOException("No repository found");
    }

    public String getPath( File file ){
        return getRepository().getWorkTree().toURI().relativize( file.toURI()).getPath();
    }

    public String getFileAsString(String filePath, RevCommit revCommit) throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // and then one can the loader to read the file
        copyRevisionFileToOutputStream( filePath, revCommit, bos);
        return bos.toString( StandardCharsets.UTF_8 );
    }

    public void copyRevisionFileToOutputStream(String filePath, RevCommit revCommit, OutputStream os) throws Exception {
        try (TreeWalk treeWalk = new TreeWalk( getRepository()) ) {
            treeWalk.addTree(revCommit.getTree());
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(filePath));
            if (!treeWalk.next()) {
                throw new IllegalStateException("Did not find expected file '" + filePath + "'");
            }

            final ObjectId objectId = treeWalk.getObjectId(0);
            final ObjectLoader loader = getRepository().open(objectId);

            loader.copyTo(os);
        }
    }

    public List<RevCommit> log(String filePath, boolean showAll) throws GitAPIException {
        final List<RevCommit> result = new ArrayList<>();
        for (RevCommit revCommit : showAll ? log().call() : log().addPath( filePath ).call()) {
            result.add(revCommit);
        }
        return result;
    }

    public String loadProjectFromRevision(String filePath, RevCommit revCommit) throws Exception {
        return getFileAsString( filePath, revCommit);
    }



}
