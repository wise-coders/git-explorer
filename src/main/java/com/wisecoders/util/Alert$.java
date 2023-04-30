package com.wisecoders.util;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.Callback;

import java.util.Optional;

public class Alert$ extends Alert {

    public Alert$(){
        super( AlertType.INFORMATION );
        getDialogPane().getStyleClass().add( "dialog-large");
        setTitle("Information Dialog");
        setResizable( true );
        getDialogPane().getScene().getWindow().setOnCloseRequest(ev -> hide());
    }

    public Alert$( AlertType type ){
        super( type );
        getDialogPane().getStyleClass().add( "dialog-large");
        getDialogPane().setMaxWidth( 700 );
        setResizable( true );
        getDialogPane().getScene().getWindow().setOnCloseRequest(ev -> hide());
    }

    public Alert$ withOwner(Scene owner ){
        if ( owner != null && owner.getWindow() != null ) {
            initOwner(owner.getWindow());
        }
        return this;
    }
    public Alert$ withTitle( String title ){
        setTitle( title );
        return this;
    }
    public Alert$ withHeaderText( String title ){
        setHeaderText( title );
        return this;
    }

    public Alert$ withButtonType( ButtonType type, String text ) {
        if ( !getButtonTypes().contains( type )) getButtonTypes().add( type );
        ((Button)getDialogPane().lookupButton( type )).setText( text );
        return this;
    }
    public Alert$ withHelpButton(String text, Callback<Void,Void> action ){
        getDialogPane().getButtonTypes().add(Dialog$.HELP_BUTTON_TYPE);
        final Button button = (Button) getDialogPane().lookupButton( Dialog$.HELP_BUTTON_TYPE );
        if( button != null ) {
            button.setText(text);
            button.setOnAction( (ev)-> action.call(null));
        }
        return this;
    }

    public Alert$ withContextText( String text ) {
        return withContextText( text, true, null );
    }

    public Alert$ withContextText( String text, boolean useScrollPane, String contentLogo ){
        if ( text != null && text.startsWith("<html>")){
            final Label htmlLabel = new Label(text );
            htmlLabel.setPrefSize( 650, 400);
            if (contentLogo == null) {
                getDialogPane().setContent(htmlLabel);
            } else {
                final BorderPane pane = new BorderPane( htmlLabel );
                final ImageView logo = Rx.getImageView( contentLogo );
                BorderPane.setMargin(logo, new Insets(10, 10, 10, 0));
                pane.setLeft(logo);
                getDialogPane().setContent( pane);
            }
        } else {
            setContentText(text);
        }
        return this;
    }


    public Alert$ withException(Throwable ex ) {
        if ( ex != null ){
            withDetailsText( ex.getLocalizedMessage() );
        }
        return this;
    }

    public Alert$ withDetailsText( String detail ){

        final TextArea textArea = new TextArea( detail);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        final Label label = new Label("Exception Stacktrace:");
        final GridPane grid = new GridPane();
        grid.setVgap( 5 );
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.add(label, 0, 0);
        grid.add(textArea, 0, 1);

        getDialogPane().setExpandableContent(grid);
        return this;
    }

    public ButtonBar.ButtonData getDialogResult(){
        Optional<ButtonType> result = showAndWait();
        return ( result.isPresent() ? result.get().getButtonData() : ButtonType.CANCEL.getButtonData() );
    }
}
