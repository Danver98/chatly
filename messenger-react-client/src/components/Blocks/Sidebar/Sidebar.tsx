import { Link, useLocation } from "react-router-dom";
import { Drawer, List, ListItem, ListItemButton, ListItemText,
    Toolbar, Box } from "@mui/material";
import { useAuthStore } from "../../../stores/authStore";

const drawerWidth = '10vw';

export default function Sidebar() {
    const accessToken = useAuthStore((state) => state.accessToken);
    const location = useLocation();

    const authenticatedLinks = [
        { text: "Intro", path: "/intro" },
        { text: "Home", path: "/secured/" },
        { text: "Chats", path: "/secured/chats" },
    ];

    const guestLinks = [
        { text: "Intro", path: "/intro" },
        { text: "Login", path: "/login" },
        { text: "Register", path: "/register" },
    ];

    const links = accessToken ? authenticatedLinks : guestLinks;

    return (
        <Drawer
            variant="permanent"
            sx={{
                width: drawerWidth,
                flexShrink: 0,
                '& .MuiDrawer-paper': {
                    width: drawerWidth,
                    boxSizing: 'border-box',
                    top: '64px', // Assumes your Header is 64px tall
                    height: 'calc(100% - 64px)',
                    borderRight: '1px solid',
                    borderColor: 'divider',
                    backgroundColor: 'grey.50',
                },
            }}
        >
            <Box sx={{ overflow: 'auto', mt: 1 }}>
                <List>
                    {links.map((link) => (
                        <ListItem key={link.text} disablePadding sx={{ mb: 0.5 }}>
                            <ListItemButton 
                                component={Link} 
                                to={link.path}
                                selected={location.pathname === link.path}
                                sx={{
                                    // Style the selected item to match your theme
                                    '&.Mui-selected': {
                                        backgroundColor: 'primary.light', // Light blue background
                                        color: 'primary.contrastText',    // White text
                                        '&:hover': {
                                            backgroundColor: 'primary.main',
                                        }
                                    },
                                    borderRadius: '0 24px 24px 0', // Modern pill shape on the right
                                    mr: 1, // Margin on the right so the pill doesn't touch the edge
                                }}
                            >
                                <ListItemText 
                                    primary={link.text} 
                                    slotProps={{
                                        primary: { fontWeight: location.pathname === link.path ? 'bold' : 'normal' }
                                    }}
                                />
                            </ListItemButton>
                        </ListItem>
                    ))}
                </List>
            </Box>
        </Drawer>
    );
}