<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>Diamond-Server Administration Console Login</title>
    <c:url var="logoutUrl" value="/login.do">
        <c:param name="method" value="logout"/>
    </c:url>
    <script>
        if (window.parent && window.parent.length != 0) {
            window.parent.location.href = "${logoutUrl}";
        }
    </script>
</head>
<body>
<c:import url="/jsp/common/message.jsp"/>
<div align='center'>
    <c:url var="url" value="/login.do">
        <c:param name="method" value="login"/>
    </c:url>

    <form method='post' action="${url}">
        <table>
            <tr>
                <td>Username:</td>
                <td><input type='text' name="username"/></td>
            </tr>
            <tr>
                <td>Password:</td>
                <td><input type='password' name="password"/></td>
            </tr>
            <tr>
                <td colspan="2" align='center'>
                    <input type="submit" value="Login"/>
                </td>
            </tr>
        </table>
    </form>
</div>
</body>
</html>
