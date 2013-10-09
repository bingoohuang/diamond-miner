<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>Diamond-Server Console users</title>
    <script type="text/javascript">
        function confirmForDelete() {
            return window.confirm("Are you sure to delete the user??");
        }

        function changePassword(user, link) {
            var newPass = window.prompt("Please input the new password:");
            if (newPass == null || newPass.length == 0)
                return false;
            link.href = link.href + "&password=" + newPass;
            return window.confirm("Are you sure to change" + user + "'s password to" + newPass + "??");
        }

    </script>
</head>
<body>
<c:import url="/jsp/common/message.jsp"/>
<center><h1><strong>Users</strong></h1></center>
<p align='center'>
    <c:if test="${userMap!=null}">
<table border='1' width="800">
    <tr>
        <td>Username</td>
        <td>Password</td>
        <td>Operations</td>
    </tr>
    <c:forEach items="${userMap}" var="user">
        <tr>
            <td>
                <c:out value="${user.key}"/>
            </td>
            <td>
                <c:out value="${user.value}"/>
            </td>
            <c:url var="changePasswordUrl" value="/admin.do">
                <c:param name="method" value="changePassword"/>
                <c:param name="userName" value="${user.key}"/>
            </c:url>
            <c:url var="deleteUserUrl" value="/admin.do">
                <c:param name="method" value="deleteUser"/>
                <c:param name="userName" value="${user.key}"/>
                <c:param name="password" value="${user.value}"/>
            </c:url>
            <td>
                <a href="${changePasswordUrl}" onclick="return changePassword('${user.key}',this);">Change password</a>&nbsp;&nbsp;&nbsp;
                <a href="${deleteUserUrl}" onclick="return confirmForDelete();">Delete</a>&nbsp;&nbsp;&nbsp;
            </td>
        </tr>
    </c:forEach>
</table>
</c:if>
</p>
<p align='center'>
    <a href="<c:url value='/jsp/admin/user/new.jsp' />">New User</a> &nbsp;&nbsp;&nbsp;&nbsp;<a
        href="<c:url value='/admin.do?method=reloadUser' />">Reload Users</a>
</p>
</body>
</html>