NFC Relaying
===========

NFC (Near Field Communication), precisely the transmission standard ISO 14443, allows contactless exchange of data over short distances. NFC Relaying enables an attacker to transmit and forward NFC commands over distances of variable length. With recently issued credit and debit cards in Austria (contactless EMV), potential attackers can use NFC Relaying to pay with the card of another person, for a total sum of 125 euros (5x25 euros) without the need of a PIN.

With our proof of concept implementation, which only utilizes standard soft- and hardware, we were able to successfully relay various ATM cards. Precisely, our proof of concept uses Galaxy Nexus smart phones. The contactless smart card first communicates with a smart phone which acts as a reader. This reader forwards the data of the smart card through some kind of transmission medium and protocol - such as UMTS or WLAN - to another smart phone, which emulates the smart card based on the received data. Finally, the emulator communicates with the payment terminal and the payment transaction can be completed.

The distance between smart card and NFC reader must be approximately 1-3 cm, although special hardware can increase the distance up to 15 cm.

Although we only tested two different terminals, it can be assumed that this attack works with other terminals as well.

A possible attack scenario could look like this: An attacker lingers within an area with many people (for example metro, tram, shopping center, etc.) and places his NFC reader smart phone secretly near the wallet of another person. Another attacker is at a payment terminal and pays various products indirectly through the smart card of the victim.

In order to prevent such attacks, a PIN should also be mandatory for amounts below 25 euros. Additionally, the timings on the terminals should be restricted in such a way that relaying won't be as easy to execute as it currently is. As a countermeasure for individual users, protective sleeves could be used which guard against unauthorized reading of smart cards.

### Setup

The following hardware is required for the relay setup:

1. Android phone with NXP PN544 NFC chip (e.g. Samsung Galaxy Nexus, Nexus S) with CyanogenMod (9 or later) (emulator)
2. NFC capable Android phone (stock Android is sufficient) (reader)

### Proof of Concept

This video shows a relayed payment transaction. The phones are connected via UMTS and an openvpn server in Germany.
https://www.youtube.com/watch?v=t0MCFjYHieQ

The software implementation logs all APDUs and is able to modify them (at the moment with hardcoded regex), so it can be used for protocol analysis.

This proof of concept was made during a semester project at the University of Applied Sciences Upper Austria, Campus Hagenberg (Secure Information Systems).
