<?php
/*
 * This file is part of Cumulus software system developed at SE Labs, Amrita University.
*
* Cumulus is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 3.0 of the License, or (at your option) any later version.
*
* Cumulus is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with Libav; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
*/

require 'config.php';
require 'utils.php';

$username = $_POST["username"];
$password = $_POST["password"];

$pdo = config_get_db();
($pdo == null && die("PDO creation failed"));

$query = "select * from user_info where username='$username';";

$res = $pdo->query($query);

if($res->rowCount() > 0)
{
	echo "<h2> User $username already exists</h2>";
	echo "<h3><a href=newuser.html>Add another user</a></h3>";	
	exit;
}

$raw_salt = utils_get_random(8);

$raw_secret = hash('SHA256', $password, true);
$raw_salted_secret = hash('SHA256', $raw_secret . $raw_salt, true);
$hex_salt = bin2hex($raw_salt);
$hex_salted_secret = bin2hex($raw_salted_secret);
$query = "insert into user_info(username, salted_secret, salt) values('$username', 0x$hex_salted_secret, 0x$hex_salt);";

if($pdo->exec($query) === 1)
{
	echo "<h2> User $username inserted</h2>";
}else
{
	echo "<h2>User $username creation failed</h2>";
}
echo "<h3><a href=newuser.html>Add more users</a></h3>"
?>