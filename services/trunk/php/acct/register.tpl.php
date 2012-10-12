
<style type='text/css'>
  @import url('register.css');
  .error, .error a { color: orange; }
</style>

<div id="">

<h2>Register <?if(isset($reg_group)):?>to Join Group <?=htmlentities($reg_group->name)?><?endif;?></h2>

<?if(isset($_GET['dod'])):?>
<div style='border: solid 1px black; background-color: #ffffee; padding: 10px; margin: 10px 0px; max-width: 600px;'>
    <strong>Note:</strong> Dicom On Demand is a trial service and provides only <strong>temporary</strong> 
    storage of imaging data.  Please see the <a href='http://www.medcommons.net/termsofuse.php'>Terms of Use</a> 
    for more information.
</div>
<?endif;?>

<?if(isset($dup_email_error)):?>
<span class='error'>
<?=$dup_email_error?>
</span>
<?endif;?>

<p>Please fill out the following form to create a MedCommons Account.</p>
<br>

<form method='post' action='register.php' id='login' name='login'>
  <div class='left'>
    <div id='registerContents' class='hdcolor'>
    <?php
      if (isset($next)) {
    ?>
        <input type='hidden' name='next' value='<?php echo $next; ?>' />
    <?php
    }
    ?>
    <?if(isset($activationKey)):?>
        <input type='hidden' name='ActivationKey' value='<?=htmlentities($activationKey)?>'>
    <?endif;?>
    <?if(isset($_GET['dod'])):?>
        <input type='hidden' name='dod' value='true'>
    <?endif;?>
        <table border='0'>
         <tr>
          <th>
           <label for='fn'>First Name</label>
          </th>
          <th>
           <label for='ln'>Last Name</label>
          </th>
         </tr>
         <tr>
          <td>
           <input class='infield'  type='text' name='fn' id='fn' value='<?= $fn ?>' >
          </td>
          <td>
           <input class='infield'  type='text' name='ln' id='ln'
                  value='<?= $ln ?>' >
          </td>
          <td>&nbsp;</td>
         </tr>
         <tr id='p_email'>
          <th><label>Email *</label></th>
          <td><input class='infield'  type='text' name='email' id='email'
            <?if(isset($fixedEmail)):?>
              readonly="true"  style="color: #888; background-color: #f6f6f6;"
            <?endif;?>
                   value='<?= $email ?>' >
           </td>
           <td class='error'>
    <?if(isset($email_error)):?>
          <?php echo $email_error; ?>
    <?endif;?>
          </td>
         </tr>
         <tr id='p_pw1'>
            <th>
               <label>Password *</label>
            </th>
            <td>
              <input class='infield'  type='password' name='pw1' id='pw1'>
            </td>
            <td class='error'> 
              <?if (isset($pw1_error)):?> <?php echo $pw1_error; ?> <?endif;?>
            </td>
          </tr>
          <tr>
            <th><label>Password (again) *</label></th>
            <td><input class='infield'  type='password' name='pw2' id='pw2' ></td>
            <td class='error'> <?if(isset($pw2_error)):?> <?php echo $pw2_error; ?> <?endif;?></td>
          </tr>
          </table>
          <div style='margin-left: 2em;'>
    
          <p id='p_termsOfUse'>
          <label>
            <input class='infield'  type='checkbox' name='termsOfUse' id='termsOfUse'>
            I have read and understand the
            <a target='_new' href="http://www.medcommons.net/terms.html">
              Terms Of Use
            </a>
          </label>
          <?php
    if (isset($tou_error)) {
    ?>
          <div class='error'>
            <?php echo $tou_error; ?>
          </div>
    <?php
    }
    ?>
        </p>
    
    <br/>
        <input  class='mainwide' type='submit' value='Register' >
        </div>
      </div>
      </div>
    </form>
    <script type="text/javascript">
    document.login.fn.focus();
    </script>
    <?php
    
    if (isset($db_error)) {
      echo "<p class='error'>";
      echo $db_error;
      echo "</p>";
    }
    
    ?>
</div>
<div id='tablewrapper'>
<table class='tinst' style=''>
  <tbody>
    <tr>
      <th>Why Register?</th>
      <td>
      <?if(isset($reg_group)):?>
        HIPAA requires that we track access to Personal Health Information and that passwords are never shared.
        You can invite other authorized users to the group and each of them will also be required to register. 
        Your email will not be shared beyond MedCommons.
      <?else:?>
        Registering takes only seconds and allows you to upload, view and share Medical Images with others.
        &nbsp;You won't be billed and no credit card or other payment information is necessary.
      <?endif;?>
      </td>
    </tr>
  </tbody>
</table>
</div>
