package com.wisecoders.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import java.util.List;

public class VBox$ extends VBox {


    public VBox$ withGap(){
        this.setSpacing( 10 );
        return this;
    }

    public VBox$ alignedCenter(){
        setAlignment(Pos.CENTER);
        return this;
    }

    public VBox$ withGap( int gap ){
        this.setSpacing( gap );
        return this;
    }

    public VBox$ withGapAndPadding() {
        return withGap().withPadding();
    }
    public VBox$ withSmallGapAndPadding() {
        setPadding( GridPane$.insetsSmall );
        setSpacing( 10 );
        return this;
    }
    public VBox$ withPadding(){
        this.setPadding( GridPane$.insets );
        return this;
    }

    public VBox$ withPadding( int padding){
        this.setPadding( new Insets( padding, padding, padding, padding));
        return this;
    }

    public VBox$ withChildren( List<? extends Node> children ){
        getChildren().addAll( children );
        return this;
    }

    public VBox$ withChildren( Node... children ){
        getChildren().addAll( children );
        return this;
    }

    public VBox$ withChildren( Node children ){
        getChildren().add( children );
        return this;
    }

}
