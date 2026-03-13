const API = {
    baseURL: '/api',

    async request(url, options = {}) {
        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json',
            }
        };

        // Check if auth is available (may not be on public pages)
        if (typeof auth !== 'undefined' && auth.getToken) {
            const token = auth.getToken();
            if (token) {
                defaultOptions.headers['Authorization'] = `Bearer ${token}`;
            }
        }

        try {
            const response = await fetch(`${this.baseURL}${url}`, {
                ...defaultOptions,
                ...options,
                headers: {
                    ...defaultOptions.headers,
                    ...options.headers
                }
            });

            if (!response.ok) {
                const error = await response.json().catch(() => ({ message: 'An error occurred' }));
                throw error;
            }

            // Handle empty responses
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return response.json();
            }
            return null;
        } catch (error) {
            if (error instanceof TypeError && error.message.includes('fetch')) {
                throw { message: 'Network error. Please check your connection.' };
            }
            throw error;
        }
    },

    get(url) {
        return this.request(url);
    },

    post(url, data) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    put(url, data) {
        return this.request(url, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    },

    delete(url) {
        return this.request(url, {
            method: 'DELETE'
        });
    }
};

// Lowercase alias for compatibility
const api = API;