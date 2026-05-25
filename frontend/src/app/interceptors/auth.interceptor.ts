import {
  HttpInterceptorFn
} from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {

  const token = localStorage.getItem('token');

  console.log('🔐 Auth Interceptor - Token:', token ? `${token.substring(0, 20)}...` : 'NO TOKEN');
  console.log('🔐 Token has periods (valid JWT):', token ? token.includes('.') : 'N/A');

  // Only send token if it looks like a valid JWT (contains dots)
  if (token && token.includes('.')) {

    const clonedReq = req.clone({

      setHeaders: {
        Authorization: `Bearer ${token}`
      }

    });

    console.log('📡 Sending request with valid JWT');

    return next(clonedReq);
  }

  if (token) {
    console.log('⚠️ Token found but not a valid JWT (no dots), sending without auth');
  } else {
    console.log('⚠️ No token found');
  }

  return next(req);
};