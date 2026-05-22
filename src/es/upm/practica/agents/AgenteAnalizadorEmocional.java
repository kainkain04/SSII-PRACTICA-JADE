package es.upm.practica.agents;

import es.upm.practica.agentLauncher.AgentBase;
import es.upm.practica.agentLauncher.AgentModel;
import es.upm.practica.common.ContentResult;
import es.upm.practica.common.MoodResult;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agente encargado de analizar el estado emocional del texto recibido.
 *
 * Rceibe un ContentResult desde el agente interfaz mediante un mensaje REQUEST,
 * analiza el texto usando un sistema de palabras clave por emoción, y devuelve
 * un MoodResult con la emoción dominante, las puntuaciones obtenidas y una
 * explicación textual. Si no hay nada claro, devuelve NEUTRO
 */
public class AgenteAnalizadorEmocional extends AgentBase {

    private static final long serialVersionUID = 1L;

    public static final String NICKNAME = "moodAnalyzer";

    // ====================== PALABRAS CLAVE POR EMOCIÓN ======================

    // Cada lista contiene términos que, si aparecen en el texto, suman 1 punto
    // a la emoción correspondiente. Se buscan como palabras completas (sin distinguir mayúsculas).
        //Alegría
    private static final List<String> PALABRAS_ALEGRIA = Arrays.asList(
        "feliz", "felicidad", "alegre", "alegría", "contento", "contenta", "celebrar",
        "celebración", "reir", "risa", "risas", "divertido", "divertida", "fiesta",
        "euforia", "eufórico", "éxito", "triunfo", "victoria", "ganó", "gano",
        "fantástico", "maravilloso", "increíble", "genial", "estupendo", "perfecto",
        "emocionante", "entusiasmo", "optimismo", "esperanza", "ilusión", "festivo",
        "sorpresa", "premio", "recompensa", "logro", "logros", "boda", "nacimiento",
        "cumpleaños", "vacaciones", "diversión", "amor", "enamorado", "enamorada"
    );

        //Tristeza
    private static final List<String> PALABRAS_TRISTEZA = Arrays.asList(
        "triste", "tristeza", "llorar", "lloro", "lágrimas", "pena", "dolor",
        "sufrimiento", "sufrir", "perdida", "pérdida", "muerte", "fallecido",
        "fallecida", "murió", "murio", "duelo", "luto", "soledad", "solo", "sola",
        "abandonado", "abandonada", "desamor", "ruptura", "fracaso", "decepción",
        "decepcionado", "melancolía", "melancólico", "nostalgia", "añoranza",
        "desesperanza", "desesperación", "abatimiento", "depresión", "deprimido",
        "angustia", "angustiado", "desánimo", "desanimado", "amargura", "amargo",
        "lamento", "lamentable", "desgracia", "infeliz", "desconsuelo"
    );

        //Calma
    private static final List<String> PALABRAS_CALMA = Arrays.asList(
        "calma", "tranquilo", "tranquila", "tranquilidad", "paz", "sereno", "serena",
        "serenidad", "relajado", "relajada", "relajación", "descanso", "descansar",
        "reposar", "reposo", "equilibrio", "silencio", "meditación", "meditar",
        "respirar", "suave", "lento", "lenta", "pausado", "pausada", "sosegado",
        "sosegada", "sosiego", "armonía", "armónico", "natural", "naturaleza",
        "contemplar", "contemplación", "reflexión", "reflexivo", "pacífico",
        "paciente", "paciencia", "templanza", "estabilidad", "estable", "plácido",
        "plácida", "apacible", "sencillo", "sencilla", "sencillez"
    );

        //Tensión
    private static final List<String> PALABRAS_TENSION = Arrays.asList(
        "tensión", "tenso", "tensa", "nervioso", "nerviosa", "nervios", "estrés",
        "estresado", "estresada", "agitado", "agitada", "agitación", "urgente",
        "urgencia", "peligro", "peligroso", "amenaza", "amenazante", "conflicto",
        "enfrentamiento", "pelea", "guerra", "ataque", "crisis", "caos", "pánico",
        "panico", "miedo", "terror", "alerta", "alarma", "violencia", "violento",
        "explosión", "explosion", "accidente", "catástrofe", "desastre", "tragedia",
        "colapso", "furia", "furioso", "rabia", "ira", "agresivo", "presión",
        "urgentemente", "desesperado", "desesperada", "temor", "inquietud",
        "inquieto", "inquieta", "intranquilo", "intranquila", "perturbado"
    );

    // ====================== SETUP ======================

    @Override
    protected void setup() {
        super.setup();

            //Registramos el agente en el DF con el tipo MOOD_ANALYSIS para ser visibles en el agente Interfaz
        registerService(AgentModel.MOOD_ANALYSIS);

            //Añadimos el comportamiento principal, que se queda esperando peticiones de análisis
        addBehaviour(new AnalizarEmocionBehaviour());

        log("Agente analizador emocional listo.");
    }

    // ====================== COMPORTAMIENTO PRINCIPAL ======================
    /*
     * Se queda en bucle esperando mensajes REQUEST del interfaz, 
     * con un ContentResult, analiza el texto y responde con un MoodResult.
     *
     * Luego, se implementa el filtro de mensajes en modo bloqueante:
     * usamos un MessageTemplate para que solo nos lleguen REQUEST y 
     * llamamos a block() cuando no hay nada pendiente,
     * así el hilo no se queda dando vueltas sin hacer nada.
     */
    private class AnalizarEmocionBehaviour extends CyclicBehaviour {

        private static final long serialVersionUID = 1L;

            //Filtro bloqueante: solo procesamos mensajes REQUEST
        private final MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

        @Override
        public void action() {

                //Se intenta recibir un mensaje REQUEST
            ACLMessage request = receive(template);

                //Si no hay mensaje, bloqueamos el comportamiento hasta que llegue uno, evitando
            if (request == null) {
                block();
                return;
            }

            log("Mensaje REQUEST recibido de: " + request.getSender().getLocalName());

            try {
                    //Extraemos el objeto del mensaje, esperando un ContentResult
                Object content = request.getContentObject();

                    //Comprobamos que el contenido sea un ContentResult
                if (!(content instanceof ContentResult)) {
                    enviarFailure(request, "El contenido recibido no es un ContentResult.");
                    return;
                }

                ContentResult contentResult = (ContentResult) content;
                String texto = contentResult.getText();

                    //Comprobamos que el texto no esté vacío
                if (texto == null || texto.trim().isEmpty()) {
                    enviarFailure(request, "El texto recibido está vacío.");
                    return;
                }

                log("Analizando texto de " + texto.length() + " caracteres.");

                    //Analizamos el texto y obtenemos las puntuaciones por emoción (contamos las palabras clave)
                Map<String, Integer> scores = calcularScores(texto);

                    //Determinamos la emoción dominante
                String mood = determinarMood(scores);

                    //Generamos la explicación textual para el user
                String explanation = generarExplicacion(mood, scores);

                    //Construimos el MoodResult con los tres campos y lo volvemos a enviar de vuelta
                MoodResult moodResult = new MoodResult(mood, scores, explanation);

                    //Preparamos la respuesta INFORM
                ACLMessage response = request.createReply();
                response.setPerformative(ACLMessage.INFORM);
                response.setContentObject(moodResult);
                send(response);

                log("Análisis finalizado: " + mood + " | Puntuaciones: " + scores);

            } catch (UnreadableException e) {
                enviarFailure(request, "No se pudo leer el objeto ContentResult del mensaje.");
                logError("Error al deserializar ContentResult", e);

            } catch (Exception e) {
                enviarFailure(request, "Error inesperado en el análisis emocional.");
                logError("Error inesperado en AnalizarEmocionBehaviour", e);
            }
        }
    }

    // ====================== ANÁLISIS ======================

    /**
     * Calcula las puntuaciones de cada emoción contando cuántas palabras clave
     * de cada lista aparecen en el texto.
     *
     * El texto se normaliza (minúsculas, sin tildes) antes de buscar.
     * Cada palabra clave que aparece como token independiente suma 1 punto.
     *
     * @param texto texto extraído por el agente de percepción
     * @return mapa con la puntuación de cada emoción
     */
    private Map<String, Integer> calcularScores(String texto) {

            //Normalizamos el texto: minúsculas y sin tildes
        String textoNorm = normalizarTexto(texto);

            //Dividimos el texto en tokens (palabras), separando por espacios y signos de puntuación
        List<String> tokens = Arrays.asList(textoNorm.split("[\\s\\p{Punct}]+"));

        Map<String, Integer> scores = new HashMap<>();
        scores.put("ALEGRIA",  contarCoincidencias(tokens, PALABRAS_ALEGRIA));
        scores.put("TRISTEZA", contarCoincidencias(tokens, PALABRAS_TRISTEZA));
        scores.put("CALMA",    contarCoincidencias(tokens, PALABRAS_CALMA));
        scores.put("TENSION",  contarCoincidencias(tokens, PALABRAS_TENSION));
        scores.put("NEUTRO",   0); // NEUTRO no tiene palabras clave, es el resultado por defecto

        return scores;
    }

    /**
     * Recorre la lista y cuenta cuántos tokens del texto coinciden con alguna 
     * palabra de la lista dada.
     *
     * @param tokens lista de palabras del texto ya normalizado
     * @param palabras lista de palabras clave de una emoción
     * @return número de coincidencias encontradas
     */
    private int contarCoincidencias(List<String> tokens, List<String> palabras) {
        int count = 0;
        for (String token : tokens) {
            if (palabras.contains(token)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Determina la emoción dominante a partir de las puntuaciones.
     *
     * Reglas:
     * -Si todas las emociones están a 0 → NEUTRO
     * -Si hay empate entre dos o más emociones con la puntuación más alta → NEUTRO
     * -En caso contrario → la emoción con mayor puntuación
     *
     * @param scores mapa de puntuaciones por emoción
     * @return nombre de la emoción dominante (en mayúsculas, sin tildes)
     */
    private String determinarMood(Map<String, Integer> scores) {

            //Buscamos la puntuación máxima entre las emociones reales (sin contar NEUTRO)
        int maxScore = 0;
        for (String emocion : Arrays.asList("ALEGRIA", "TRISTEZA", "CALMA", "TENSION")) {
            maxScore = Math.max(maxScore, scores.getOrDefault(emocion, 0));
        }

            //Si la puntuación máxima es 0, no se ha detectado nada → NEUTRO
        if (maxScore == 0) {
            return "NEUTRO";
        }

            //Contamos cuántas emociones tienen la puntuación máxima (para detectar empates) (si hay empate, es NEUTRO)
        int empates = 0;
        String candidato = "NEUTRO";
        for (String emocion : Arrays.asList("ALEGRIA", "TRISTEZA", "CALMA", "TENSION")) {
            if (scores.getOrDefault(emocion, 0) == maxScore) {
                empates++;
                candidato = emocion;
            }
        }

            //Si hay empate entre varias emociones → NEUTRO
        if (empates > 1) {
            return "NEUTRO";
        }

        return candidato;
    }

    /**
     * Genera una explicación textual del resultado del análisis.
     *
     * La explicación incluye la emoción detectada y un resumen de las
     * puntuaciones obtenidas para que el usuario entienda por qué se llegó
     * a ese resultado.
     *
     * @param mood emoción detectada
     * @param scores puntuaciones obtenidas por emoción
     * @return texto explicativo
     */
    private String generarExplicacion(String mood, Map<String, Integer> scores) {

        String resumenScores = "Puntuaciones — "
                + "Alegría: " + scores.getOrDefault("ALEGRIA", 0)
                + ", Tristeza: " + scores.getOrDefault("TRISTEZA", 0)
                + ", Calma: " + scores.getOrDefault("CALMA", 0)
                + ", Tensión: " + scores.getOrDefault("TENSION", 0)
                + ".";

        switch (mood) {
            case "ALEGRIA":
                return "El texto analizado contiene términos asociados a emociones positivas como alegría, "
                        + "celebración o entusiasmo, siendo esta la emoción más presente en el contenido. "
                        + resumenScores;

            case "TRISTEZA":
                return "El texto analizado refleja un tono emocional negativo, con términos relacionados "
                        + "con tristeza, pérdida o melancolía como los más frecuentes. "
                        + resumenScores;

            case "CALMA":
                return "El texto analizado transmite una sensación de tranquilidad o serenidad, "
                        + "predominando términos ligados a la calma, el descanso o la paz. "
                        + resumenScores;

            case "TENSION":
                return "El texto analizado presenta un tono de tensión o intensidad emocional, "
                        + "con términos asociados a estrés, conflicto, peligro o urgencia como los más presentes. "
                        + resumenScores;

            case "NEUTRO":
            default:
                return "El análisis no ha detectado una emoción dominante clara en el texto. "
                        + "Puede deberse a que el contenido es descriptivo o informativo, "
                        + "o a que varias emociones están presentes con una intensidad similar. "
                        + resumenScores;
        }
    }

    // ====================== UTILIDADES ======================

    /**
     * Normaliza un texto eliminando tildes y convirtiéndolo a minúsculas,
     * para que la comparación con las palabras clave no falle por el formato.
     *
     * @param texto texto original
     * @return texto normalizado
     */
    private String normalizarTexto(String texto) {
        if (texto == null) return "";

        return texto.toLowerCase()
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace("ü", "u")
                .replace("ñ", "n");
    }

    /**
     * Envía un mensaje FAILURE al agente solicitante cuando no se puede
     * completar el análisis emocional correctamente.
     *
     * @param request mensaje original recibido
     * @param errorMessage descripción del error
     */
    private void enviarFailure(ACLMessage request, String errorMessage) {
        try {
            ACLMessage response = request.createReply();
            response.setPerformative(ACLMessage.FAILURE);
            response.setContent(errorMessage);
            send(response);
            log("Enviado FAILURE: " + errorMessage);
        } catch (Exception e) {
            logError("No se pudo enviar FAILURE", e);
        }
    }
}
