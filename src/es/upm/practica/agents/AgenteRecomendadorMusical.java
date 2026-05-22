package es.upm.practica.agents;

import es.upm.practica.agentLauncher.AgentBase;
import es.upm.practica.agentLauncher.AgentModel;
import es.upm.practica.common.MoodResult;
import es.upm.practica.common.PlaylistResult;
import es.upm.practica.common.Song;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Agente encargado de recomendar música en función del estado de ánimo detectado.
 *
 * Este agente recibe un objeto MoodResult desde el agente
 * analizador emocional. A partir de la emocion recibida, lee una base local de canciones
 * en formato CSV, filtra las canciones que coinciden con ese estado de ánimo y
 * devuelve una playlist con varias recomendaciones.
 */

public class AgenteRecomendadorMusical extends AgentBase {

    private static final long serialVersionUID = 1L;

    public static final String NICKNAME = "musicRecommender";

    private static final String CSV_PATH = "data/canciones.csv";
    private static final int NUM_RECOMMENDATIONS = 3;
    
    /**
     * Método setup del agente.
     *
     * Se ejecuta una sola vez cuando el agente arranca. Aquí se registra el servicio
     * en el Directory Facilitator y se añade el comportamiento que estará esperando
     * peticiones de recomendación musical.
     */
    
    @Override
    protected void setup() {
        super.setup();

        // Registramos el servicio de recomendación musical en el DF
        // Así, otros agentes pueden encontrar este agente buscando el servicio MUSIC_RECOMMENDATION
        registerService(AgentModel.MUSIC_RECOMMENDATION);

        // Añadimos el comportamiento principal del agente.
        // Este comportamiento se encargará de recibir peticiones y responder con playlists
        addBehaviour(new RecomendarMusicaBehaviour());

        log("Agente recomendador musical listo.");
    }

    /**
     * Comportamiento cíclico del agente recomendador.
     *
     * Al ser un CyclicBehaviour, se ejecuta continuamente mientras el agente esté vivo.
     * En cada iteración comprueba si ha recibido un mensaje REQUEST. Si no hay mensaje,
     * se bloquea para no consumir recursos innecesariamente.
     */
    
    private class RecomendarMusicaBehaviour extends CyclicBehaviour {

        private static final long serialVersionUID = 1L;

        @Override
        public void action() {

        	// Definimos un filtro para recibir únicamente mensajes de tipo REQUEST
            // Así evitamos procesar mensajes que no correspondan a una petición de recomendación
            MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage request = receive(template);

            if (request == null) {
                block();
                return;
            }

            try {
            	// Obtenemos el contenido del mensaje, esperamos recibir un MoodResult
                Object content = request.getContentObject();

                if (!(content instanceof MoodResult)) {
                    enviarFailure(request, "El contenido recibido no es un MoodResult.");
                    return;
                }

                MoodResult moodResult = (MoodResult) content;
                
                // Obtenemos el estado de ánimo detectado
                String mood = normalizarMood(moodResult.getMood());

                log("Recibido mood para recomendar música: " + mood);

                // Leemos todas las canciones disponibles desde el fichero CSV y filtramos por emoción obtenida
                List<Song> allSongs = leerCancionesDesdeCSV();
                List<Song> recommendedSongs = recomendarPorMood(allSongs, mood);
                
                String explanation;

                // Si no hay canciones para el mood recibido, usamos NEUTRO como alternativa.
                if (recommendedSongs.isEmpty() && !mood.equals("NEUTRO")) {
                	String originalMood = mood;

                	mood = "NEUTRO";
                	recommendedSongs = recomendarPorMood(allSongs, mood);

                if (recommendedSongs.isEmpty()) {
                    enviarFailure(request, "No se han encontrado canciones para el mood recibido ni para NEUTRO.");
                    return;
                 }

                 explanation = generarExplicacionMoodNoDisponible(originalMood, recommendedSongs);

                 log("No había canciones para " + originalMood + ". Enviada playlist NEUTRO como alternativa.");
               } else {
                 	if (recommendedSongs.isEmpty()) {
                 		enviarFailure(request, "No se han encontrado canciones para el mood: " + mood);
                 		return;
                 }

                 explanation = generarExplicacion(mood, recommendedSongs);
               }

                // Creamos el objeto PlaylistResult que se enviará como respuesta
                PlaylistResult playlist = new PlaylistResult(mood, recommendedSongs, explanation);

                ACLMessage response = request.createReply();
                
                // Indicamos que la respuesta es de tipo INFORM porque contiene un resultado correcto
                response.setPerformative(ACLMessage.INFORM);
                
                // Enviamos la playlist como objeto serializable
                response.setContentObject(playlist);
                
                // Enviamos la respuesta al agente que hizo la petición
                send(response);

                log("Playlist enviada correctamente con " + recommendedSongs.size() + " canciones.");

            } catch (UnreadableException e) {
                enviarFailure(request, "No se ha podido leer el objeto recibido.");
                logError("Error leyendo el contenido del mensaje", e);

            } catch (IOException e) {
                enviarFailure(request, "No se ha podido leer el fichero de canciones.");
                logError("Error leyendo canciones.csv", e);

            } catch (Exception e) {
                enviarFailure(request, "Error inesperado en el recomendador musical.");
                logError("Error inesperado", e);
            }
        }
    }

    /**
     * Lee el fichero CSV de canciones y convierte cada línea en un objeto Song.
     *
     * El formato esperado del CSV es:
     * id;title;artist;genre;mood;description
     *
     * @return lista de canciones leídas del fichero CSV
     * @throws IOException si hay un problema al leer el fichero
     */
    
    private List<Song> leerCancionesDesdeCSV() throws IOException {
        List<Song> songs = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(CSV_PATH))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {

            	// Saltamos la primera línea porque contiene las cabeceras del CSV
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                // Ignoramos líneas vacías
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Dividimos la línea usando punto y coma como separador
                // El -1 mantiene campos vacíos si los hubiera
                String[] fields = line.split(";", -1);

                // Si la línea no tiene todos los campos necesarios, la ignoramos
                if (fields.length < 6) {
                    log("Línea ignorada por formato incorrecto: " + line);
                    continue;
                }

                Song song = new Song(
                        fields[0].trim(),	//id
                        fields[1].trim(),	//title
                        fields[2].trim(),	//artist
                        fields[3].trim(),	//genre
                        normalizarMood(fields[4].trim()),	//mood
                        fields[5].trim()	//description
                );

                songs.add(song);
            }
        }

        return songs;
    }
    
    /**
     * Filtra las canciones según el mood recibido.
     *
     * Se devuelven las primeras 3 canciones que coinciden en el CSV.
     *
     * @param allSongs lista completa de canciones
     * @param mood estado de ánimo detectado
     * @return lista de canciones recomendadas
     */

    private List<Song> recomendarPorMood(List<Song> allSongs, String mood) {
        return allSongs.stream()
                .filter(song -> normalizarMood(song.getMood()).equals(mood))
                .limit(NUM_RECOMMENDATIONS)
                .collect(Collectors.toList());
    }
    
    /**
     * Genera una explicación personalizada según el estado de ánimo detectado.
     *
     * La idea es que la interfaz no muestre solo una lista de canciones, sino también
     * una justificación sencilla de por qué esas canciones encajan con el texto analizado.
     *
     * @param mood estado de ánimo detectado
     * @param songs canciones recomendadas
     * @return explicación textual de la recomendación
     */
    
    private String generarExplicacion(String mood, List<Song> songs) {

        String canciones = songs.stream()
                .map(song -> song.getTitle() + " de " + song.getArtist())
                .collect(Collectors.joining(", "));

        switch (normalizarMood(mood)) {

            case "ALEGRIA":
                return "El texto analizado transmite una emoción positiva, asociada a alegría, optimismo o celebración. "
                        + "Por eso se han seleccionado canciones con un tono animado y alegre, pensadas para acompañar "
                        + "una sensación de bienestar y energía positiva. Las canciones recomendadas son: "
                        + canciones + ".";

            case "TRISTEZA":
                return "El texto analizado presenta un tono emocional más triste o melancólico. "
                        + "Por ese motivo se recomiendan canciones con una carga emocional más profunda, que encajan con "
                        + "situaciones de pérdida, reflexión o sensibilidad. Las canciones seleccionadas buscan acompañar "
                        + "ese estado de ánimo sin romper con el tono del texto. Las canciones recomendadas son: "
                        + canciones + ".";

            case "CALMA":
                return "El texto analizado transmite una sensación de calma, tranquilidad o equilibrio. "
                        + "Por ello, el sistema recomienda canciones suaves y relajadas, adecuadas para acompañar un ambiente "
                        + "sereno y sin demasiada intensidad. La playlist propuesta busca reforzar esa sensación de paz. "
                        + "Las canciones recomendadas son: " + canciones + ".";

            case "TENSION":
                return "El texto analizado contiene un tono de tensión, intensidad o conflicto. "
                        + "Por eso se han elegido canciones con más fuerza y presencia, que encajan mejor con contenidos "
                        + "relacionados con presión, riesgo o situaciones de mayor intensidad emocional. Las canciones "
                        + "recomendadas son: " + canciones + ".";

            case "NEUTRO":
                return "El sistema no ha detectado una emoción dominante clara en el texto analizado. "
                        + "Por ese motivo se propone una playlist de carácter neutro y equilibrado, con canciones que pueden "
                        + "acompañar el contenido sin inclinarse demasiado hacia una emoción concreta. Las canciones "
                        + "recomendadas son: " + canciones + ".";

            default:
                return "A partir del estado de ánimo detectado en el texto, el sistema ha seleccionado canciones que "
                        + "comparten una etiqueta emocional similar. La recomendación se basa en la relación entre el mood "
                        + "del texto y el mood asociado a cada canción en la base de datos. Las canciones recomendadas son: "
                        + canciones + ".";
        }
    }
    
    /**
     * Genera una explicación cuando el mood recibido no está contemplado
     * en la base de canciones y se usa NEUTRO como alternativa.
     *
     * @param originalMood mood recibido inicialmente
     * @param songs canciones neutras recomendadas
     * @return explicación textual de la recomendación alternativa
     */
    private String generarExplicacionMoodNoDisponible(String originalMood, List<Song> songs) {

        String canciones = songs.stream()
                .map(song -> song.getTitle() + " de " + song.getArtist())
                .collect(Collectors.joining(", "));

        return "El sistema ha recibido el estado de ánimo " + originalMood
                + ", pero no hay canciones asociadas a esa emoción en la base de datos. "
                + "Para evitar dejar la recomendación vacía, se propone una playlist neutra y equilibrada, "
                + "pensada para acompañar el texto sin asociarlo a una emoción concreta. "
                + "Las canciones recomendadas son: " + canciones + ".";
    }
    
    /**
     * Normaliza el estado de ánimo para evitar problemas de comparación.
     *
     * Por ejemplo:
     * - "alegría" se convierte en "ALEGRIA"
     * - " Tristeza " se convierte en "TRISTEZA"
     *
     * @param mood estado de ánimo original
     * @return estado de ánimo normalizado
     */

    private String normalizarMood(String mood) {
        if (mood == null) {
            return "NEUTRO";
        }

        String normalized = mood.trim().toUpperCase();

        //Quitamos tildes por si acaso
        normalized = normalized
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U");

        return normalized;
    }

    /**
     * Envía un mensaje FAILURE al agente que hizo la petición.
     *
     * Se usa cuando el recomendador no puede completar correctamente la recomendación,
     * por ejemplo porque el contenido recibido no es válido o porque no hay canciones
     * para el mood detectado.
     *
     * @param request mensaje original recibido
     * @param errorMessage explicación del error
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