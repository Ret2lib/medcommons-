<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Quick Test Appliance <?=$_SERVER['SERVER_NAME']?></title>
</head>
<body>
 <img src='collateral/mclogo40htrans.png' height=30px />
 <p>iPhone Stuff: <a href='m/'>iPhone Web App</a>    <a href='http://ci.myhealthespace.com/probe/sintaxi-phonegap-23e3fed563301cbaf6cf4f21dc1eb5c7e7e12a10/'
 >Phone Gap Version</a> (right click to download)
 </p>
<h1>Test This MedCommons Appliance <?=$_SERVER['SERVER_NAME']?></h1>
<p>This page is for the MedCommons Help Desk to diagnose particular problems with a customer's MedCommons experience. A separate, simpler page for customers is <a href=selftest.html >here</a>.
</p>
<p>All of these tests resolve to putting up the standard small mc performance display, and recycling every 10 seconds or every minute, depending upon the test</p>
<h2>Loopback Tests</h2>
<p>These tests run in and out of the MedCommons appliance as quickly as possible, some with the smallest call into MySql, or with a full login</p>
<ul>
<li><a href=loopback_basic.html>Basic Loopback</a></li>

<li><a href=loopback_mysql.html>MySql Loopback</a></li>

<li><a href=loopback_login.html>Login User</a></li>
</ul>
<div id=smaller >
<p>You can run these tests 100 times from www.medcommons.net. This keeps the script load away from the appliance under test. </p>
<ul>
<li><a href="http://www.medcommons.net/probe/www/api/client/niltest.php?count=100&appliance=<?='http://'.$_SERVER['SERVER_NAME'] ?>">basic</a></li>
<li><a href="http://www.medcommons.net/probe/www/api/client/minmysqltest.php?count=100&appliance=<?='http://'.$_SERVER['SERVER_NAME'] ?> ">mysql</a></li>
<li><a href="http://www.medcommons.net/probe/www/api/client/logintest.php?count=100&appliance=<?='http://'.$_SERVER['SERVER_NAME'] ?>">login</a></li>
</ul>
</div>

<h2>Upload Tests</h2>
<p>Some of these tests can take a long time. To get a very precise measure, run the "Upload Nothing" test first, and subtract its round-trip time from
the specific upload test result to get the actual time to move the data upstream.</p>
<ul>


<li><a href=loopback_basic.html>Upload Nothing</a></li>

<li><a href=upload_1K.html>Upload 1K Bytes</a></li>

<li><a href=upload_128K.html>Upload 128K Bytes</a> (one minute cycle)</li>


<li><a href=upload_1M.html>Upload 1M Bytes</a> (one minute cycle) very slow!!!</li>

<li><a href=upload_16M.html>Upload 16M Bytes</a> (two minute cycle) very very slow!!!!</li>
</ul>
<h2>Download Tests</h2>
<p>More coming soon</p>
<ul>
<li><a href=download_10M.html>Download 10M Bytes</a> (two minute cycle) very slow!!!!</li>
</ul>
<h2>Load Tests</h2>
<p><i>Seen in www/details.html</i> - These show the load on the appliance, direct from the bowels of Linux to your screen</p>
<ul>
<li><a href=load_average.html>Load Average</a></li>
</ul>
<h2>Database Counters</h2>
<p><i>Seen in www/details.html</i> - These show what's going on in the MedCommons DB. It has a high impact on large MedCommons systems due to many count(*) operations.</p>
<ul>
<li><a href=db_counters.html>MedCommons DB Counters</a></li>
</ul>


</body>
</html>