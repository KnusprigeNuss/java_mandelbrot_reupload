package at.tugraz.oop2.shared;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

public class ColorModeProperty {

    private static final Property<ColourModes> selected_mode = new SimpleObjectProperty<>();


    public static Property<ColourModes> optionProperty() {
        return selected_mode;
    }

    public ColourModes getSelectedMode() {
        return optionProperty().getValue();
    }

    public void setSelectedMode(ColourModes selected_mode) {
        optionProperty().setValue(selected_mode);
    }
}
