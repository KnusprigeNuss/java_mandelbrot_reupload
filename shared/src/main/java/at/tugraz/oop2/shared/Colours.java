package at.tugraz.oop2.shared;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Colours {

    public static StringProperty colour1 = new SimpleStringProperty();
    public static StringProperty colour2 = new SimpleStringProperty();


    public static void setColour1(String colour) {
        colour1.setValue(colour);
    }

    public static String getColour1() {
        return colour1.getValue();
    }

    public static void setColour2(String colour) {
        colour2.setValue(colour);
    }

    public static String getColour2() {
        return colour2.getValue();
    }
}
