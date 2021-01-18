package fractal.models;

import fractal.Fractal;

/**
 * classe que contem o array do tipo Fractal com os 4 fractais:
 * <p>- Burning Ship;
 * <p>- Mandelbrot;
 * <p>- Julia;
 * <p>- Julia (com coordenadas diferentes).
 */
public class Index {
    public Fractal[] fractals;

    /**
     * Inicializa o array pela ordem:
     * <p>- Burning Ship;
     * <p>- Mandelbrot;
     * <p>- Julia;
     * <p>- Julia (com coordenadas diferentes).
     */
    public Index() {
        fractals = new Fractal[]{
                new BurningShip(),
                new Mandelbrot(),
                new Julia(),
                new Julia2()
        };
    }
}
