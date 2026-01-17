-- Generate API keys for existing status_apps and status_components

-- Create a function to generate random API keys
CREATE OR REPLACE FUNCTION generate_api_key() RETURNS VARCHAR(64) AS $$
DECLARE
    chars TEXT := 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_';
    result VARCHAR(64) := '';
    i INTEGER;
BEGIN
    FOR i IN 1..43 LOOP
        result := result || substr(chars, floor(random() * length(chars) + 1)::integer, 1);
    END LOOP;
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- Generate API keys for all status_apps that don't have one
UPDATE status_apps SET api_key = generate_api_key() WHERE api_key IS NULL;

-- Generate API keys for all status_components that don't have one
UPDATE status_components SET api_key = generate_api_key() WHERE api_key IS NULL;

-- Drop the function as it's no longer needed
DROP FUNCTION IF EXISTS generate_api_key();
