package es.upm.practica;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;

public class AgentePercepcion extends Agent {

    protected void setup() {
        System.out.println("Hola! El agente de percepcion " + getLocalName() + " ya esta listo.");
        
        addBehaviour(new LeerNoticiaBehaviour());
    }

    protected void takeDown() {
        System.out.println("El agente de percepcion " + getLocalName() + " va a finalizar.");
    }

    private class LeerNoticiaBehaviour extends OneShotBehaviour {

        public void action() {
            System.out.println("Agente " + getLocalName() + ": Intentando leer el articulo...");
            
            // Aquí añadiremos el código para conectarnos a la URL y sacar el texto
            
            System.out.println("Agente " + getLocalName() + ": Lectura finalizada.");
        }
    }
}