<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>New Config</title>

    <script src="../../../js/prototype_for_validation.js" type="text/javascript"></script>
    <script src="../../../js/effects.js" type="text/javascript"></script>
    <script src="../../../js/validation_cn.js" type="text/javascript"></script>
    <style type="text/css">
        body td {
            color: #333;
            font-family: Arial, Helvetica, sans-serif;
            font-size: 10pt;
        }

        .validation-advice {
            margin: 0px 0;
            padding: 0px;
            margin-left: 10px;
            color: #FF3300;
            font-weight: bold;
            display: inline;
        }
    </style>
</head>
<body>
<c:import url="/jsp/common/message.jsp"/>
<center><h1><strong>New Config</strong></h1></center>
<p align='center'>
    <c:url var="postConfigUrl" value="/admin.do">
        <c:param name="method" value="postConfig"/>
    </c:url>

<form action="${postConfigUrl}" method="post" id="config-form">
    <table align='center'>
        <tr>
            <td>dataId:</td>
            <td>
                <input type='text' name='dataId' width="256" class="required max-length-128"/>
            </td>
        </tr>
        <tr>
            <td>group:</td>
            <td><input type='text' name='group' width="256" class="required max-length-128" value="DEFAULT_GROUP"/></td>
        </tr>
        <tr>
            <td>valid:</td>
            <td><input type='checkbox' name='valid' checked="true"></td>
        </tr>
        <tr>
            <td colspan="2">content:</td>
        </tr>
        <tr>
            <td colspan="2">
                <textarea cols="100" rows="10" name="content" class="required max-length-65535"></textarea>
            </td>
        </tr>
        <tr>
            <td colspan="2">description:</td>
        </tr>
        <tr>
            <td colspan="2">
                <textarea cols="100" rows="10" name="description" class="max-length-65535"></textarea>
            </td>
        </tr>
        <tr>
            <td colspan="2"><input type="submit" value="Submit"/>
        </tr>
    </table>
</form>
</p>
<script type="text/javascript">
    new Validation('config-form', {immediate: true});
</script>
</body>
</html>