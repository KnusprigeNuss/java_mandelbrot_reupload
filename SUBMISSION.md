# Group 152
>David Knill,  
Alexander Niederreiter,  
Anton Schleinitz,  
Verena Schaffer
<br>  

## Design Decisions  
### Gui
* Most of our functionalities are found in the **FractalApplication**  
* In the start function first the parameters are parsed
* We used the given pictures of the sample GUI as reference for our GUI, only small adjustments have been made:
  * Added a "minus" Button inside the connection Editor. Clicking on a connection followed by this button will remove the connection. 
  * Added UI for the Bonus task below the regular UI-Elements.
    * Different Options for the second Fractal.
    * Two Color Pickers to select colors which are used when using the option "Custom Color".
* Some design decisions for the GUI:
  * Parameters are displayed inside the TextFields and dynamically updated using bidirectional bindings.
  * Changing the values in the TextFields will instantly rerender the Fractal with the new value (with some exceptions, like invalid input).
  * Adding an invalid connection will add the standard value.
  * Adding the same connection multiple times is possible.
  * Connection Editor only allows a single connection to be added at a time.
  * Typing any invalid Character inside the TextFields will restore the default value of that TextField.
  * Iterations lower than one and Power smaller than two is not possible to be entered. Default values are restored.
  * When TextField is empty the value is not changed, if the TextField is not focused anymore default value is restored.
  * If a "-" is entered for a negative value, the value is not changed until a valid negative number is entered,
if no valid negative value is added and the TextField is not focused anymore it will return to its default value.
* Scrolling and Panning was implemented according to the README:
  * For Panning we used part of the Fractal Calculation Formula to calculate the delta of the Fractal center based on two mouse coordinates.
* The constant parameters of the Julia fractal were synced using bindings.
* To configure the connection we used the given pictures for inspiration:
  * Clicking the "Connection Editor" Button opens a new window.
    * This new window shows currently configured connections in a list.
    * You can add a connection with the "+" button.
      * A new window with a Textfield appears.
      * Entering a connection and pressing enter adds the connection.
      * If the connection is not a valid the default connection gets added.
    * You can remove a connection by clicking on it in the list and then clicking the "-" button. You do not have to save.
    * Clicking "Cancel" will revert all changes.
    * Clicking "Save" will save all changes.
    * Clicking "Check connections" will open a window with information about working connections.
      * Clicking "OK" inside this window will close the window.


### Timer thread
The timer thread is a class that we implemented to increase performance by avoiding the spam of rendercalls.
Once one of the listeners reports that one of the values got changed we take the current values of the mandelbrot- and
juliaset propertyclasses and after 500 ms we get the values again and compare them.
If these two instances aren't the same, we start the 500 ms again. Once the two instances are the same, we know that no more
changes were made and therefore can make a call to render the sets. To avoid rendering both sets even when its not
needed we added an Integer which shows which set should be rendered. 1 stands for mandelbrot, 2 stands for 
juliaset and 3 stands for both. When panning and zooming the 500 ms time limit doesn't get triggered and therefore
the speed/performance will only be influenced by the rendering time.

### Additional classes
We also added some additional classes mainly in the shared section. To name a few:
We made a property class for
* the fractals in general
* the mandelbrot set
* the julia set

in which we safe the current values of out sets and to add listeners to them.
We also added property classes for ColorModes and RenderMode.
For the bonus task, we also expended FractalType and ColourModes. To safe the two custom
colours, we also added a Colours class.


### Calculation
* The calculation works according to the `README.md`  
    * Interpreting the README:   
    * iterating n times means loop from 0 to n-1,  
    * for the magnitude you have to calculate the square root, but you could shortcut it and compare it to 4, instead of root  
    * for the julia calculation there is an extra check for 0 iterations    
* Within the calculation the rendering of the image can be split up into several simple-images  
    * The splitting is done in **horizontal stripes**,
      those are later concatenated into one SimpleImage
    * Simply a **starting point** of the new stripe has to be passed to the calculation  
* The colour is immediately set within the calculation for each pixel, which can later be put onto a canvas
<br>

* The Workers work as following:
  todo
<br>

### Bonus  
* Colour Picker with added ColourMode CUSTOM  
* Disco Mode in ColourMode Selection  
* Extra Fractals:  
    * Test via choosing the fractal for the second canvas (drop-down-menu / combo-box)  
    * You might have to change parameters or drag the Multibrot around, for it to look cool  
    * There is one Newton Fractal with a polynom, one with a sine  
    * We implemented BurningShip and BurningBird and those are also implemented with ties to the Multibrot 
    * For a cool fractal: BURNING_SHIP | iterations = 6 | power = 5 (froggo)
<br>

## Open Issues   
* Maximizing the Window does not work, as the canvas sizes do not get updated
<br>
<br>





