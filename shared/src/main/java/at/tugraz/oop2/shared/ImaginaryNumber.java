package at.tugraz.oop2.shared;

// import lombok.Getter
// @Getter

public class ImaginaryNumber {
    public double getRealpart() {
        return realpart;
    }

    private double realpart;

    public double getImaginarypart() {
        return imaginarypart;
    }

    private double imaginarypart;

    @SuppressWarnings("unused")
    public ImaginaryNumber(){
        realpart = 0;
        imaginarypart = 0;
    }
    public ImaginaryNumber(double a, double b){
        realpart = a;
        imaginarypart = b;
    }

    public double polarFormPhi(){
        return Math.atan2(imaginarypart,realpart);
    }

    public double polarFormAbsZ(){
        return Math.sqrt(this.magnitude_squared());
    }
    public double magnitude(){
        return this.polarFormAbsZ();
    }

    public double magnitude_squared(){ // better for performance reasons
        return Math.pow(this.realpart,2) + Math.pow(this.imaginarypart,2);
    }

    public void copyValues(ImaginaryNumber a){
        this.realpart = a.realpart;
        this.imaginarypart = a.imaginarypart;
    }

    public void setValues(double r, double i){
        this.realpart = r;
        this.imaginarypart = i;
    }

    public void add(ImaginaryNumber z2){
        this.realpart += z2.realpart;
        this.imaginarypart += z2.imaginarypart;
    }

    public ImaginaryNumber add(ImaginaryNumber z, boolean a){
        if(a){
            this.add(z);
        }
        return this;
    }


    public void powToK(double k){
        double abs_z = this.polarFormAbsZ(); // absolute value of z
        double abs_raised_to_k = Math.pow(abs_z,k); // absolute value raised to k
        double phi = this.polarFormPhi(); // angle of z
        double phi_times_k = phi * k;
        this.realpart = abs_raised_to_k * Math.cos(phi_times_k);
        this.imaginarypart = abs_raised_to_k * Math.sin(phi_times_k);
    }

    public ImaginaryNumber powToK(double k, boolean a){
        if(a){
            this.powToK(k);
        }
        return this;
    }


    public ImaginaryNumber division(ImaginaryNumber bottom){
        if(bottom.realpart == 0 && bottom.imaginarypart == 0){
            return bottom;
        }

        ImaginaryNumber quer = new ImaginaryNumber(bottom.getRealpart(), -1 * bottom.getImaginarypart());
        ImaginaryNumber multiplication_result = this.multiplication(quer);

        double divisor = Math.pow(bottom.getRealpart(),2) + Math.pow(bottom.getImaginarypart(),2);

        ImaginaryNumber res = new ImaginaryNumber(multiplication_result.getRealpart()/divisor,
                multiplication_result.getImaginarypart()/divisor);

        return res;
    }

    public ImaginaryNumber multiplication(ImaginaryNumber two){
        double one_real = this.realpart;
        double one_imaginary = this.imaginarypart;
        double two_real = two.getRealpart();
        double two_imaginary = two.getImaginarypart();

        double first = one_real * two_real - one_imaginary * two_imaginary;
        double second = one_real * two_imaginary + one_imaginary * two_real;

        ImaginaryNumber ret = new ImaginaryNumber(first,second);
        return ret;
    }

    public ImaginaryNumber multiplication(double immediate){
        ImaginaryNumber ret = new ImaginaryNumber(this.realpart * immediate, this.imaginarypart * immediate);
        return ret;
    }

    public ImaginaryNumber sine(){
        double real = Math.sin(this.realpart) * Math.cosh(this.imaginarypart);
        double imaginary = Math.cos(this.realpart) * Math.sinh(this.imaginarypart);
        ImaginaryNumber ret = new ImaginaryNumber(real, imaginary);
        return ret;
    }


}
