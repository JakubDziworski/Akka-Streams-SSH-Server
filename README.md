# Akka-Streams-SSH-Server

[![Build Status](https://travis-ci.org/JakubDziworski/Akka-Streams-SSH-Server.svg?branch=master)](https://travis-ci.org/JakubDziworski/Akka-Streams-SSH-Server)

This project implements Diffie-Hellman key exchange for securing websocket connection (exactly like ssh does).
It secures the connection but does not (yet) understand commands (it will just respond with 'I've received <received_msg>')

Implemented according to [RFC4253 Section 8](https://tools.ietf.org/html/rfc4253#section-8)

The best way to get hang of this implementation is to look at `SshConnectionTest.scala`

