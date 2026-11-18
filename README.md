# Simple Proximity Chat

This mod changes the text chat. I always found it strange that there wasn't something to stop the text chat for voice chat and radios; otherwise, what's the point?

## Proximity chat

*   Messages only reach players within Simple Voice Chat's voice distance.
*   Up to 75% of that distance, messages arrive intact. Beyond that, words start dropping out (`see you ____ the ____`) until the message is lost completely.
*   The chat event is never cancelled. The mod only decides which players each message is delivered to, so the server console and other mods that read chat still receive every message. This is to avoid possible problems with other mods reading the chat.

## Radio support (Simple Radio)

**Walkie-talkies and transceivers**

*   Hold one in either hand, or wear it in a Curios slot, and your messages are also sent over its frequency and modulation (FM/AM).
*   A radio kept in your normal inventory does nothing, which is how you go back to local chat only.
*   Messages are received by anyone with an active radio on the same channel, and by players standing within its speaker range.
*   Writing next to another player's active radio transmits through it as well, the same way it would pick up your voice.

**Radio towers**

*   Writing within range of a switched-on Microphone sends your message through its wiring to the connected Transmitters.
*   Receivers and Radios tuned to that frequency pass it on to their Speakers, and players within speaker range can read it.
*   Handheld radios and towers work together in both directions.

**Signal and range**

*   Range and static use Simple Radio's own formula and configuration values: transmission power, reception power, antennas, diminishment and diminish threshold.
*   As the signal weakens, more words are lost. Radio messages are shown in amber so they are easy to tell apart from local chat.

## HUD icons

A small icon above the Simple Voice Chat microphone shows your current radio state:

*   📻 **Radio**: you can transmit and receive
*   🎙️ **Microphone**: you can only transmit (for example, next to a tower microphone)
*   🔊 **Speaker**: you can only receive (for example, next to a speaker or radio block)

The icon follows Voice Chat's HUD position, scale and hide-icons settings.

## Compatibility

| Mod                    | Status   | What it adds                                                                                                                            |
| ---------------------- | -------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| Simple Voice Chat      | Required | Chat range                                                                                                                              |
| Simple Radio           | Optional | Walkie-talkies, transceivers and radio towers                                                                                           |
| Curios API             | Optional | Radios in Curios slots for constant long-distance text chat.                                                                            |
| VibrativeVoice         | Optional | Chat messages create weak vibrations for sculk sensors and the Warden, both where they are written and where they are played by a radio |
| Beautified Chat Server | Optional | Distant and radio messages keep its formatting, including ranks, timestamps and name colours                                            |

 

## Configuration

There is no separate config file. Chat range comes from Simple Voice Chat, and everything radio-related comes from Simple Radio's configuration, so the text and voice settings always match.

## Installation

*   Required on the **server**.
*   Recommended on **clients** for the HUD icons. Players without the mod can still join and chat normally; they just won't see the icons.

## Commands

`/simpleproximitychat debug` lists the voice distance, each player's active radios (type, frequency, modulation and power), and how a message from you would reach every online player right now. It is useful for checking radio setups.
