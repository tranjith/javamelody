﻿README Java Melody : monitoring d'applications Java EE
******************************************************

Cet outil de monitoring a pour but de monitorer les serveurs d'applications Java / Java EE en recette et en production.

Ce n'est pas un outil de simulation de requêtes utilisateur,
c'est un outil de mesure et de statistiques sur le fonctionnement réel d'une application
selon l'usage qui en est fait par les utilisateurs.

Le monitoring est en grande partie basé sur des statistiques de requêtes et sur des courbes d'évolution.
Il permet ainsi d'améliorer les applications en recette et en production et d'aider à :
	- factualiser les temps de réponse moyens et les nombres d'exécutions
	- prendre des décisions quand les tendances sont mauvaises, avant que les problèmes ne soient trop graves
	- optimiser sur la base des temps de réponse les plus pénalisants
	- trouver les causes à l'origine des temps de réponse
	- vérifier l'amélioration réelle après des optimisations

Auteur : Emeric Vernat, Bull (emeric.vernat@bull.netASLicence : LGPL
URL : http://javamelody.googlecode.com/
Version Java requise en ex6 minimum,
	JDK ou JRE d'Oracle/DK ou JJRockit d'Oracle/BEA ou J9 d'IBM d'Oracle/BEA
Version de serveur requise en exécution : api servlet 2.4 minimum (ou JavaEE 1.4), 
	com, 6 ou 7, GlassFish v2 ou v3, JBoss 4, 5, 6 ou 7, Jonas 4 ou 5, Jetty 6 ou 7, WebLogic 9, 10 ou 11Logic 9 ou 10
Dépendance requise : JRobin (LGPL) pour les courbes d'évolution
Dépendances optionne2.1.7elles : iText (LGPL ou MPL) pour les rapports au format pdf en plus de html,
	Spring AOP, AOP alliance, Spring core, Spring beans et Commons logging pour le monitoring des,
	Ehcache pour le monitoring des caches, Quartz pour le monitoring des jobs en batchs beans Spring
Langag, anglais, allemand, portugais et chinoisisNavigateur : Le rapport html de JavaMelody est optimisé pour Firefox, Chrome ou MSIE8 (MSIE7 non recommandé).is
