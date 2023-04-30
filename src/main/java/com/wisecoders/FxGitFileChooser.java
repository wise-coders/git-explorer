package com.wisecoders;

import com.wisecoders.util.Action;
import com.wisecoders.util.Dialog$;
import com.wisecoders.util.HBox$;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ComboBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.prefs.Preferences;

public class FxGitFileChooser extends Dialog$<File> {

    private final ComboBox<String> fileCombo = new ComboBox<>();

    private final ArrayList<String> knownFiles = new ArrayList<>();
    private final Preferences pref = Preferences.systemNodeForPackage(Long.class);

    public FxGitFileChooser(Scene owner){
        super( owner);
        int i = 0;
        String fileName;
        while ( ( fileName = pref.get("gitFile" + i, null)) != null ){
            knownFiles.add( fileName );
            i++;
        }
        fileCombo.setPrefWidth(300);
        fileCombo.getItems().addAll( knownFiles );
        setResultConverter((dialogButton) -> dialogButton != null && dialogButton.getButtonData() == ButtonBar.ButtonData.OK_DONE ? getResult() : null );

    }

    @Override
    public Node createContentPane() {
        return new HBox$().withGap().withChildren(fileCombo, rx.button("chooseFile"));
    }

    @Override
    public void createButtons() {
        createOkButton();
    }

    @Override
    public boolean apply() {
        if ( fileCombo.getValue() != null ){
            setResult( new File( fileCombo.getValue() ));
        }
        return true;
    }

    @Action
    public void chooseFile(){
        File file = new FileChooser().showOpenDialog(getDialogScene().getWindow());
        if ( file != null ){
            if ( !knownFiles.contains( file.getAbsolutePath() )){
                knownFiles.add( file.getAbsolutePath());
                int i = 0;
                for ( String fileName : knownFiles ){
                    pref.put("gitFile" + i, fileName);
                    i++;
                }
            }
            fileCombo.setValue( file.getAbsolutePath());
        }
    }
}
