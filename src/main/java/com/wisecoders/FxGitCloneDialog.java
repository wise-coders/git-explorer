package com.wisecoders;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.wisecoders.util.*;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory;
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.io.Writer;

public class FxGitCloneDialog extends Dialog$ {

    private final TextField urlField = new TextField();
    private final TextField directoryField = new TextField();

    private final CheckBox sshCheckBox;
    private final TextField sshPrivateKeyTextField;
    private final PasswordField sshParaphraseField = new PasswordField();
    private final PasswordField sshPasswordField = new PasswordField();

    private final Label privateKeyLabel = rx.label("privateKeyLabel");
    private final Label paraphraseLabel = rx.label("paraphraseLabel");
    private final Label passwordLabel = rx.label("passwordLabel");

    public FxGitCloneDialog(Window owner){
        super(owner);
        sshCheckBox = rx.checkBox("sshCheckBox");
        sshPrivateKeyTextField = rx.textField("privateKey");
        sshCheckBox.selectedProperty().addListener((o,p,c)-> refreshSSH( !c ));
        refreshSSH( true );
    }

    private void refreshSSH( boolean disable ){
        privateKeyLabel.setDisable(disable);
        passwordLabel.setDisable(disable);
        paraphraseLabel.setDisable(disable);
        sshPrivateKeyTextField.setDisable( disable );
        sshParaphraseField.setDisable( disable );
        sshPasswordField.setDisable( disable );
    }


    @Override
    public Node createContentPane() {
        HBox.setHgrow( directoryField, Priority.ALWAYS );
        HBox.setHgrow( sshPrivateKeyTextField, Priority.ALWAYS);
        urlField.setPrefColumnCount( 40 );
        return new GridPane$().withGapAndPadding().
                add( rx.label("urlLabel"), "0,0,r,c").
                add( urlField, "1,0,f,c").
                add( rx.label("directoryLabel"), "0,1,r,c").
                add( new HBox$().withChildren( directoryField, rx.button("chooseDirectory")), "1,1,f,c").
                add( sshCheckBox, "0,2,r,c").
                add( privateKeyLabel, "0,3,r,c").
                add( new HBox$().withChildren( sshPrivateKeyTextField, rx.button("choosePrivateKey")), "1,3,f,c").
                add( paraphraseLabel, "0,4,r,c").
                add( sshParaphraseField, "1,4,f,c").
                add( passwordLabel, "0,5,r,c").
                add( sshPasswordField, "1,5,f,c");
    }

    @Override
    public void createButtons() {
        createActionButton("clone");
        createCancelButton();
    }

    @Override
    public boolean apply() {
        return !StringUtil.isEmpty(urlField.getText()) && !StringUtil.isEmpty(directoryField.getText());
    }

    @Action
    public void chooseDirectory(){
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File selectedDirectory = directoryChooser.showDialog( getDialogScene().getWindow() );
        if ( selectedDirectory != null ) {
            directoryField.setText( selectedDirectory.getAbsolutePath() );
        }
    }

    @Action
    public void choosePrivateKey(){
        File file = new FileChooser().showOpenDialog( getDialogScene().getWindow() );
        if ( file != null ) {
            sshPrivateKeyTextField.setText( file.getAbsolutePath() );
        }
    }

    @Action
    public Task<Git> clone(){
        return new Task<>() {
            @Override
            protected Git call() throws Exception {
                final Writer writer = new Writer(){
                    @Override
                    public void write(char[] cbuf, int off, int len) {
                        updateMessage( new String(cbuf));
                    }

                    @Override
                    public void flush() {}

                    @Override
                    public void close() {}
                };
                updateMessage("Running...");

                CloneCommand cloneCommand = Git.cloneRepository()
                        .setURI( urlField.getText() )
                        .setDirectory( new File(directoryField.getText()) )
                        .setProgressMonitor(new TextProgressMonitor(writer))
                        .setCredentialsProvider( new FxGitCredentialsProvider( getDialogScene() ));

                if ( sshCheckBox.isSelected() ) {
                    SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
                        @Override
                        protected void configure(OpenSshConfig.Host host, Session session) {
                            if ( StringUtil.isFilled( sshParaphraseField.getText() )){
                                session.setPassword( sshPasswordField.getText() );
                            }
                        }

                        @Override
                        protected JSch createDefaultJSch( FS fs ) throws JSchException {
                            final JSch defaultJSch = super.createDefaultJSch( fs );
                            if ( StringUtil.isFilled( sshPrivateKeyTextField.getText())) {
                                defaultJSch.addIdentity(sshPrivateKeyTextField.getText());
                            }
                            return defaultJSch;
                        }
                    };
                    cloneCommand.setTransportConfigCallback(transport -> {
                        SshTransport sshTransport = (SshTransport) transport;
                        sshTransport.setSshSessionFactory(sshSessionFactory);
                    });
                }


                return cloneCommand.call();
            }

            @Override
            protected void succeeded() {
                rx.showInformation(getDialogScene(), "GIT Clone succeed. \nNow save the model in the repository, and reopen the GIT dialog.");
            }

            @Override
            protected void failed() {
                rx.showError( getDialogScene(), getException());
            }
        };
    }



}