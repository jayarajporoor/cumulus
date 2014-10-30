-- MySQL dump 10.13  Distrib 5.1.66, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: cumulus
-- ------------------------------------------------------
-- Server version	5.1.66-0ubuntu0.10.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `nodes`
--

DROP TABLE IF EXISTS `nodes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nodes` (
  `nid` int(11) NOT NULL AUTO_INCREMENT,
  `nodename` varchar(100) NOT NULL,
  `nodeid` binary(8) NOT NULL,
  `publickey` varchar(1000) NOT NULL,
  `uid` int(11) NOT NULL,
  `last_seen` datetime DEFAULT NULL,
  `ipaddr` varchar(100) DEFAULT NULL,
  `port` int(11) DEFAULT NULL,
  `avail_space` int(11) DEFAULT NULL,
  PRIMARY KEY (`nid`),
  UNIQUE KEY `nodeid` (`nodeid`),
  UNIQUE KEY `nodename` (`nodename`),
  KEY `uid` (`uid`),
  CONSTRAINT `nodes_ibfk_1` FOREIGN KEY (`uid`) REFERENCES `users` (`uid`) ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nodes`
--

LOCK TABLES `nodes` WRITE;
/*!40000 ALTER TABLE `nodes` DISABLE KEYS */;
INSERT INTO `nodes` VALUES (16,'test-server','¡ú|ªÖ î','MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoZg8uP8T3Zc2i27eTl5q/msw1HyG5TEb1CjljmeB2x/RbpOsPri89tlMY0Q5LyCV/u+KutHRl8HCWM44RJjfm/bLyqjXPav9EhXMCX6PV9zkHrfQBe+DounBNB36YyC6StyyJ09k1+yVOAMunVVBESIdX8ChYcE3iGPzbRNky9IA3I7tN7TKk1RQaRLRghESacVduQfsTaoLwXN0w52k8rcBu9Cmd7kVJ8JjMlkLyg+n8c6u+VjyPnr/bezASaW7zx6LhQF9zkpmBZfXaNE+DDHrpcLP9PR3N738xQlcFHxK9k7K5KZaw/iONqDdT1aA6aD82bGrLvJuPQSSYuyQMQIDAQAB',2,'2012-12-09 21:14:42','192.168.160.92',9090,15000000);
/*!40000 ALTER TABLE `nodes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `points`
--

DROP TABLE IF EXISTS `points`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `points` (
  `nid` int(11) NOT NULL,
  `pid` binary(4) NOT NULL,
  PRIMARY KEY (`pid`),
  KEY `nid` (`nid`),
  CONSTRAINT `points_ibfk_1` FOREIGN KEY (`nid`) REFERENCES `nodes` (`nid`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `points`
--

LOCK TABLES `points` WRITE;
/*!40000 ALTER TABLE `points` DISABLE KEYS */;
INSERT INTO `points` VALUES (16,'Öåw'),(16,'bk('),(16,':Ô	'),(16,'F KÐ'),(16,'N.­®'),(16,'\\T;O'),(16,'fJzÎ'),(16,'iA¶i'),(16,'À¦¥8'),(16,'æGTš');
/*!40000 ALTER TABLE `points` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `uid` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(100) DEFAULT NULL,
  `salted_secret` binary(32) DEFAULT NULL,
  `salt` binary(8) DEFAULT NULL,
  `total_quota` int(11) DEFAULT NULL,
  `avail_quota` int(11) DEFAULT NULL,
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (2,'test','`ù‘²M\"^(Ä$Hva6^ÅS²Ò+ÌÎ2w½','\0$\'ä„ù¯',NULL,NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2012-12-10 11:16:18
