package com.wisecoders.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

import java.util.List;


public class HBox$ extends HBox {


    public HBox$ withGap(){
        this.setSpacing( 20 );
        return this;
    }

    public HBox$ withSmallGap(){
        this.setSpacing( 10 );
        return this;
    }

    public HBox$ withGap( int gap ){
        this.setSpacing( gap );
        return this;
    }

    public HBox$ withPadding(){
        this.setPadding( GridPane$.insets );
        return this;
    }


    public HBox$ withChildren( Node... children ){
        getChildren().addAll( children );
        return this;
    }

    public HBox$ withAlignment(Pos pos ){
        setAlignment( pos);
        return this;
    }

}

