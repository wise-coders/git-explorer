package com.wisecoders;

import com.wisecoders.util.Rx;
import javafx.scene.Scene;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

import java.util.prefs.Preferences;

public class FxGitCredentialsProvider extends CredentialsProvider {

    private Scene scene;
private final Preferences pref = Preferences.userNodeForPackage(Long.class);

    public FxGitCredentialsProvider(){
        super();
    }
    public FxGitCredentialsProvider(Scene scene ){
        super();
        this.scene = scene;
    }
    @Override
    public boolean isInteractive() {
        return true;
    }

    @Override
    public boolean supports(CredentialItem... items) {
        return true;
    }

    private boolean usedSuccessfully = false, dialogShown = false;
    private boolean retValue = false;
    @Override
    public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
        if ( dialogShown && usedSuccessfully) {
            for (CredentialItem item : items) {
                if (item instanceof CredentialItem.CharArrayType) {
                    String val = (pref.get( "git" + item.getPromptText(), null));
                    if (val == null) {
                        usedSuccessfully = false;
                    } else {
                        ((CredentialItem.CharArrayType) item).setValue( val.toCharArray() );
                    }
                } else if (item instanceof CredentialItem.StringType) {
                    String val = pref.get( "git" + item.getPromptText(), null);
                    if (val == null) {
                        usedSuccessfully = false;
                    } else {
                        ((CredentialItem.StringType) item).setValue( val );
                    }
                } else if (item instanceof CredentialItem.YesNoType) {
                    boolean val = pref.getBoolean( "gitBool" + item.getPromptText(), false);
                    ((CredentialItem.YesNoType) item).setValue( val );
                }
            }
            if (usedSuccessfully){
                return true;
            }
        }
        Rx.runAndWait( () -> retValue = new FxGitCredentialsDialog( scene.getWindow(), items ).showDialogIsResultOkDone() );
        if ( retValue ){
            dialogShown = true;
        }
        return retValue;
    }


    public void usedSuccessfully( ){
        this.usedSuccessfully = true;
    }

    public void setScene( Scene scene ){
        this.scene = scene;
    }
}

