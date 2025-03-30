package finance.project.api.entities;

import finance.project.api.model.CandleDTO;

import java.time.LocalDateTime;

import java.time.LocalDateTime;
import java.util.List;

public class HighLowSwing {
    private double high;
    private double low;
    private LocalDateTime highTime;
    private LocalDateTime lowTime;

    // Constructeur pour un point haut ou bas
    public HighLowSwing(double highOrLow, LocalDateTime highOrLowTime,boolean isHight) {
        if(isHight){
            this.high = highOrLow;
            this.highTime = highOrLowTime;
        } else {
            this.low = highOrLow;
            this.lowTime = highOrLowTime;
        }
    }


    // Accesseurs (getters)
    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public LocalDateTime getHighTime() {
        return highTime;
    }

    public LocalDateTime getLowTime() {
        return lowTime;
    }
}
