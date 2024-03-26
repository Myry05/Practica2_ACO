import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Random;

public class AntColonyOptimization extends JFrame {

    private JPanel panel;
    private JButton startButton;
    private JTextArea outputTextArea;

    private static final int NUM_CITIES = 10;
    private static final int NUM_ANTS = 10;
    private static final int MAX_ITERATIONS = 100;
    private static final double ALPHA = 1.0; // Importancia de las feromonas
    private static final double BETA = 2.0; // Importancia de la información Heurística
    private static final double RHO = 0.5; // Tasa de evaporación de las feromonas
    private static final double Q = 100; // Factor de depósito de las feromonas
    private static final double INITIAL_PHEROMONE = 1.0;

    private int[][] distanceMatrix;
    private double[][] pheromoneMatrix;
    private ArrayList<Ant> ants;

    public AntColonyOptimization() {
        setTitle("Optimizacion Colonia de Hormigas");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        panel = new JPanel(new BorderLayout());

        startButton = new JButton("Start");
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startACO();
            }
        });

        outputTextArea = new JTextArea();
        outputTextArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(outputTextArea);

        panel.add(startButton, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        getContentPane().add(panel);
    }

    private void initialize() {
        distanceMatrix = generateRandomDistanceMatrix(NUM_CITIES);
        pheromoneMatrix = new double[NUM_CITIES][NUM_CITIES];
        ants = new ArrayList<>();

        // Inicialización de los niveles de feromonas
        for (int i = 0; i < NUM_CITIES; i++) {
            for (int j = 0; j < NUM_CITIES; j++) {
                pheromoneMatrix[i][j] = INITIAL_PHEROMONE;
            }
        }

        // Inicialización de hormigas
        for (int i = 0; i < NUM_ANTS; i++) {
            ants.add(new Ant(NUM_CITIES));
        }
    }

    private void startACO() {
        initialize();
        outputTextArea.setText("");

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            for (Ant ant : ants) {
                ant.visitCities(distanceMatrix, pheromoneMatrix, ALPHA, BETA);
            }

            updatePheromone();

            Ant bestAnt = getBestAnt();
            outputTextArea.append("Iteration " + (i + 1) + ", Best tour length: " + bestAnt.getTourLength() + "\n");

            // Resetear hormigas
            for (Ant ant : ants) {
                ant.reset();
            }
        }
    }

    private void updatePheromone() {
        // Evaporacion
        for (int i = 0; i < NUM_CITIES; i++) {
            for (int j = 0; j < NUM_CITIES; j++) {
                pheromoneMatrix[i][j] *= (1 - RHO);
            }
        }

        // Depositar feromonas
        for (Ant ant : ants) {
            double tourLength = ant.getTourLength();
            for (int i = 0; i < NUM_CITIES - 1; i++) {
                int city1 = ant.getTour()[i];
                int city2 = ant.getTour()[i + 1];
                pheromoneMatrix[city1][city2] += (Q / tourLength);
                pheromoneMatrix[city2][city1] += (Q / tourLength);
            }
            // Depositar feromonas para la última arista
            pheromoneMatrix[ant.getTour()[NUM_CITIES - 1]][ant.getTour()[0]] += (Q / tourLength);
            pheromoneMatrix[ant.getTour()[0]][ant.getTour()[NUM_CITIES - 1]] += (Q / tourLength);
        }
    }

    private Ant getBestAnt() {
        Ant bestAnt = ants.get(0);
        for (Ant ant : ants) {
            if (ant.getTourLength() < bestAnt.getTourLength()) {
                bestAnt = ant;
            }
        }
        return bestAnt;
    }

    private int[][] generateRandomDistanceMatrix(int size) {
        Random random = new Random();
        int[][] matrix = new int[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = i; j < size; j++) {
                if (i == j) {
                    matrix[i][j] = 0;
                } else {
                    int distance = random.nextInt(100) + 1; // Genera una distancia aleatoria entre un rango de 1-100
                    matrix[i][j] = distance;
                    matrix[j][i] = distance;
                }
            }
        }
        return matrix;
    }

    private class Ant {
        private int[] tour;
        private double tourLength;

        public Ant(int numCities) {
            tour = new int[numCities];
            tourLength = 0;
        }

        public int[] getTour() {
            return tour;
        }

        public double getTourLength() {
            return tourLength;
        }

        public void visitCities(int[][] distanceMatrix, double[][] pheromoneMatrix, double alpha, double beta) {
            ArrayList<Integer> unvisitedCities = new ArrayList<>();
            for (int i = 0; i < NUM_CITIES; i++) {
                unvisitedCities.add(i);
            }

            int currentCity = 0; // Start from city 0
            tour[0] = currentCity;
            unvisitedCities.remove(Integer.valueOf(currentCity));

            for (int i = 1; i < NUM_CITIES; i++) {
                int nextCity = selectNextCity(currentCity, unvisitedCities, distanceMatrix, pheromoneMatrix, alpha, beta);
                tour[i] = nextCity;
                tourLength += distanceMatrix[currentCity][nextCity];
                unvisitedCities.remove(Integer.valueOf(nextCity));
                currentCity = nextCity;
            }

            tourLength += distanceMatrix[tour[NUM_CITIES - 1]][tour[0]]; // Return to the starting city
        }

        private int selectNextCity(int currentCity, ArrayList<Integer> unvisitedCities, int[][] distanceMatrix,
                                   double[][] pheromoneMatrix, double alpha, double beta) {
            double totalProbability = 0;
            double[] probabilities = new double[NUM_CITIES];
            for (int city : unvisitedCities) {
                double pheromone = Math.pow(pheromoneMatrix[currentCity][city], alpha);
                double heuristic = 1.0 / distanceMatrix[currentCity][city];
                double probability = pheromone * Math.pow(heuristic, beta);
                probabilities[city] = probability;
                totalProbability += probability;
            }

            // Seleccionar la siguiente ciudad utilizando la selección de ruleta
            double rand = Math.random() * totalProbability;
            double cumulativeProbability = 0;
            for (int city : unvisitedCities) {
                cumulativeProbability += probabilities[city];
                if (cumulativeProbability >= rand) {
                    return city;
                }
            }

            // Alternativa si no se seleccionó ninguna ciudad
            return unvisitedCities.get(0);
        }

        public void reset() {
            tourLength = 0;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new AntColonyOptimization().setVisible(true);
            }
        });
    }
}

