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

$valid_routes = array("keepalive", "create_node", "getnodes", "putblockinfo", "getblockinfo", "getnodeinfo", "delblocknode");

require 'config.php';
require 'auth.php';

$route = utils_getparam('route');

$secret = utils_getparam('secret');
$username = utils_getparam('username');

$args = array("route", "secret", "username");

foreach($args as $arg)
{
	if(!isset($$arg) || $$arg == false)
	{
		echo utils_wrap_response("error", "param_absent:$arg");
		exit;
	}
}
	
$pdo = config_get_db();
if($pdo == null)
{
	echo utils_wrap_response('error', 'db_failure');
	utils_error_log(__FILE__ ,  __LINE__ ,  "Cannot create instance of PDO");	
	exit;
}

list($uid,$errmsg) = auth_check_credentials($pdo, $username, $secret);

if($uid === false)
{
	echo utils_wrap_response("error", "auth_failed");
	utils_error_log(__FILE__ ,  __LINE__ ,  $errmsg);
	exit;
}

if(!in_array($route, $valid_routes))
{
	echo utils_wrap_response("error", "invalid_route");
	exit;
}

require "$route.php";

$route($pdo, $uid);

?>