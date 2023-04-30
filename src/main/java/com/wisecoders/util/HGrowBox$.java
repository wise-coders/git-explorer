package com.wisecoders.util;


import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;


public class HGrowBox$ extends HBox {

    public HGrowBox$(){
        HBox.setHgrow( this, Priority.ALWAYS );
    }

}

