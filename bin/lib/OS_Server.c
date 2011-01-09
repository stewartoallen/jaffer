#include "OS_Server.h"
#include <pwd.h>
#include <sys/types.h>

#if defined(__gnu_linux__) || defined(__sun__)
#define USE_SHADOW
#include <shadow.h>
#endif

#if defined(__APPLE__)
#undef USE_SHADOW
#endif

JNIEXPORT jboolean JNICALL Java_com_neuron_jaffer_OS_1Server_validUser
  (JNIEnv *env, jclass obj, jbyteArray user)
{
	jboolean result = JNI_FALSE;
	jint len = (*env)->GetArrayLength(env, user);
	jbyte *u = (*env)->GetPrimitiveArrayCritical(env, user, 0); 
	struct passwd *pwd = getpwnam(u);
	if (pwd != NULL)
	{
		result = JNI_TRUE;
	}
	(*env)->ReleasePrimitiveArrayCritical(env, user, u, 0);
	return result;
}

JNIEXPORT jboolean JNICALL Java_com_neuron_jaffer_OS_1Server_validPassword
  (JNIEnv *env, jclass obj, jbyteArray user, jbyteArray pass)
{
	jboolean result = JNI_FALSE;
	jint ulen = (*env)->GetArrayLength(env, user);
	jint plen = (*env)->GetArrayLength(env, pass);
	jbyte  *u = (*env)->GetPrimitiveArrayCritical(env, user, 0); 
	jbyte  *p = (*env)->GetPrimitiveArrayCritical(env, pass, 0); 
	char *crp;
	struct passwd *pwd = getpwnam(u);
#ifdef USE_SHADOW
	struct spwd *spw = getspnam(pwd->pw_name);
	if (spw != NULL)
	{
		pwd->pw_passwd = spw->sp_pwdp;
	}
#endif
	if (pwd != NULL && pwd->pw_passwd != NULL)
	{
		crp = (char *)crypt(p, pwd->pw_passwd);
		result = strcmp(crp, pwd->pw_passwd) ? JNI_FALSE : JNI_TRUE;
	}
	(*env)->ReleasePrimitiveArrayCritical(env, pass, p, 0);
	(*env)->ReleasePrimitiveArrayCritical(env, user, u, 0);
	return result;
}

JNIEXPORT jboolean JNICALL Java_com_neuron_jaffer_OS_1Server_switchUser
  (JNIEnv *env, jclass obj, jbyteArray user)
{
	jboolean result = JNI_FALSE;
	jint len = (*env)->GetArrayLength(env, user);
	jbyte *u = (*env)->GetPrimitiveArrayCritical(env, user, 0); 
	struct passwd *pwd = getpwnam(u);
	if (pwd != NULL && !setegid(pwd->pw_gid) && !seteuid(pwd->pw_uid))
	{
		result = JNI_TRUE;
	}
	(*env)->ReleasePrimitiveArrayCritical(env, user, u, 0);
	return result;
}

