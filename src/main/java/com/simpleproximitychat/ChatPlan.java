package com.simpleproximitychat;

import com.simpleproximitychat.radio.RadioNodes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Plan de entrega de un mensaje: cómo le llega a cada jugador, y qué walkies/altavoces
 * lo han reproducido (para las vibraciones de VibrativeVoice).
 */
public record ChatPlan(Map<UUID, Delivery> deliveries, List<RadioNodes.Output> radioOutputs) {}
