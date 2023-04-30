package com.wisecoders;

import com.wisecoders.util.ButtonDialog$;
import com.wisecoders.util.GridPane$;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Window;
import org.eclipse.jgit.transport.CredentialItem;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.Preferences;

public class FxGitCredentialsDialog extends ButtonDialog$ {

    private final GridPane$ gridPane = new GridPane$().withGapAndPadding();
    private final Map<CredentialItem, Node> inputFields = new LinkedHashMap<>();

    private final Preferences pref = Preferences.systemNodeForPackage(Long.class);
    private CheckBox rememberPasswordCheck;

    public FxGitCredentialsDialog(Window window, CredentialItem... items){
        super( window );
        setDialogTitle("Credentials Dialog");
        int y = 0;
        for ( CredentialItem item : items ) {
            gridPane.add( new Label( item.getPromptText()), "0," + y + ",r,c");
            if ( item instanceof CredentialItem.StringType ) {
                final TextField textField = new TextField();
                textField.setPrefColumnCount( 40 );
                textField.setText( pref.get( "git" + item.getPromptText(), null) );
                inputFields.put(item, textField);
                gridPane.add(textField, "1," + y + ",f,c");
                y++;
            } else if ( item instanceof CredentialItem.CharArrayType ){
                final PasswordField passwordField = new PasswordField();
                try {
                    passwordField.setText( pref.get( "git" + item.getPromptText(), null) );
                } catch ( Throwable ignore ){}
                inputFields.put(item, passwordField);
                gridPane.add(passwordField, "1," + y + ",f,c");
                y++;
                if ( rememberPasswordCheck == null ){
                    rememberPasswordCheck = rx.checkBox("rememberPasswordCheck", pref.getBoolean("gitRememberPassword", false ));
                    gridPane.add(rememberPasswordCheck, "1," + y + ",l,c");
                    y++;
                }
            } else if ( item instanceof CredentialItem.YesNoType ){
                final CheckBox checkBox = new CheckBox();
                checkBox.setSelected( pref.getBoolean("gitBool" + item.getPromptText(), false ));
                inputFields.put(item, checkBox);
                gridPane.add( checkBox, "1," + y + ",l,c");
                y++;
            } else if ( item instanceof CredentialItem.InformationalMessage ){
                gridPane.add( new Label( (item).getPromptText() ), "1," + y + ",l,c");
                y++;
            }
        }
    }

    @Override
    public Node createContentPane() {
        return gridPane;
    }

    @Override
    public void createButtons() {
        createOkButton();
        createCancelButton();
    }

    @Override
    public boolean apply() {
        for ( Map.Entry<CredentialItem, Node> entry : inputFields.entrySet()){
            final CredentialItem item = entry.getKey();
            final Node control = entry.getValue();
            boolean remember = rememberPasswordCheck != null && rememberPasswordCheck.isSelected();

            pref.putBoolean("gitRememberPassword", remember );
            if ( item instanceof CredentialItem.StringType){
                String value = ((TextField) control).getText();
                ((CredentialItem.StringType) item).setValue(value);
                pref.put( "git"+item.getPromptText(), remember ? value : null );
            } else if ( item instanceof CredentialItem.CharArrayType ){
                String value = ((PasswordField) control).getText();
                if ( value != null ) {
                    ((CredentialItem.CharArrayType) item).setValue(value.toCharArray());
                    pref.put("git" + item.getPromptText(), remember ? value : null);
                }
            } else if ( item instanceof CredentialItem.YesNoType){
                boolean selected = ((RadioButton) control).isSelected();
                ((CredentialItem.YesNoType) item).setValue(selected);
                pref.putBoolean( "gitBool"+item.getPromptText(), remember && selected);
            }
        }
        return true;
    }
}