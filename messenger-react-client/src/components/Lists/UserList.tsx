import {
    Avatar, Box, Button, Checkbox, Dialog, FormControlLabel, InputAdornment, List, ListItem, ListItemAvatar,
    ListItemText, Switch, TextField
} from "@mui/material";
import SearchIcon from '@mui/icons-material/Search';
import User from "../../models/User";
import UserService, { UserRequestDTO } from "../../services/UserService";
import { forwardRef, useCallback, useEffect, useMemo, useState, useRef } from "react";
import { DIRECTION } from "../../util/Constants";
import "./UserList.css";
import { toast } from 'sonner';

const SearchBar = ({ onChange }: { onChange: (value: string) => any }) => {
    return (
        <TextField
            id="standard-basic"
            label="Enter user credentials"
            variant="standard"
            fullWidth
            margin="normal"
            slotProps={{
                input: {
                    endAdornment: (
                        <InputAdornment position="end">
                            <SearchIcon />
                        </InputAdornment>
                    ),
                }
            }}
            onChange={(e: any) => {
                onChange(e.target.value)
            }}
        />
    )
}

const UserList = forwardRef(({ users, checked, handleToggle, multiSelect }:
    { users: User[], checked: number[], handleToggle: (value: number) => any, multiSelect?: boolean | null }, ref?: any) => {
    if (users == null || users.length === 0) {
        return (
            <>
                No users found!
            </>
        )
    }
    return (
        <Box >
            <List>
                {
                    users.map((user, index) =>
                        <ListItem
                            key={user.id}
                            ref={index === users.length - 1 ? ref : null}
                            secondaryAction={
                                <Checkbox
                                    disabled={!multiSelect && checked.length > 0 && !checked.includes(index)}
                                    checked={checked.includes(index)}
                                    onChange={(event) => handleToggle(index)}
                                />
                            }
                        >
                            <ListItemAvatar >
                                <Avatar src={user.avatar} />
                            </ListItemAvatar>
                            <ListItemText
                                primary={user.name + ' ' + user.surname}
                            >
                            </ListItemText>
                        </ListItem>
                    )
                }
            </List>
        </Box>
    )
});

const UserSelection = ({ user, requestFilter, onResult }:
    { user?: User | null, requestFilter?: object, onResult: (elements: any[], params?: any | null) => any }) => {

    const [open, setOpen] = useState(false);
    const [users, setUsers] = useState<User[]>([]);
    const [search, setSearch] = useState("");
    const [checked, setChecked] = useState<any[]>([]);
    const [multiSelect, setMultiSelect] = useState(false);
    const [chatName, setChatName] = useState<string | null>(null);
    const [lastElementRef, setLastElementRef] = useState(null);
    const observerRef = useRef<IntersectionObserver | null>(null);
    const [intersected, setIntersected] = useState(false);
    const [hasMore, setHasMore] = useState(true);
    const abortController = useMemo(() => {
        return new AbortController();
    }, []);

    const handleToggle = (position: number) => {
        if (!multiSelect && checked.length > 1) return;
        const currentIndex = checked.indexOf(position);
        const newChecked = [...checked];

        if (currentIndex === -1) {
            newChecked.push(position);
        } else {
            newChecked.splice(currentIndex, 1);
        }

        setChecked(newChecked);
    };

    const handleClickOpen = useCallback(() => {
        setOpen(true);
    }, []);

    const handeClickClose = useCallback(() => {
        setChecked([]);
        setMultiSelect(false);
        setOpen(false);
    }, []);

    const handleClickOk = useCallback(() => {
        if (multiSelect && (chatName == null || chatName === '')) {
            toast.warning(`You'd set a chat name!`);
            return;
        }
        const ids = checked.map((position) => users[position].id);
        handeClickClose();
        onResult(ids, {
            chatName,
            multiSelect
        });
    }, [users, checked, chatName, multiSelect, onResult, handeClickClose]);

    useEffect(() => {
        observerRef.current = new IntersectionObserver(
            (entries) => {
                if (entries[0].isIntersecting) {
                    setIntersected((prev) => !prev);
                }
            }
        );
    }, []); // Runs on start only

    useEffect(() => {
        const observerCurrent = observerRef.current;

        if (lastElementRef) {
            observerCurrent?.observe(lastElementRef);
        }

        return () => {
            if (lastElementRef) {
                observerCurrent?.disconnect();
            }
        };
    }, [lastElementRef]);

    useEffect(() => {
        const fetchUsers = async (dto: UserRequestDTO) => {
            if (!(open && hasMore)) return;
            if (search) {
                // In case of search is present start requesting from 3 symbols-lenght string
                if (search.length > 2) {
                    const newUsers = await UserService.list(dto, abortController);
                    setUsers((prevUsers) => [...prevUsers, ...newUsers]);
                    setHasMore(newUsers && newUsers.length > 0);
                }
            } else {
                const newUsers = await UserService.list(dto, abortController);
                // Check if answer contains data
                setUsers((prevUsers) => [...prevUsers, ...newUsers]);
                setHasMore(newUsers && newUsers.length > 0);
            }
        }
        // If we've already started a new request, cancel it
        const dto: UserRequestDTO = {
            filter: {
                ...requestFilter,
                search: search
            },
            id: users && users.length ? users[users.length - 1].id : null,
            surname: users && users.length ? users[users.length - 1].surname : null,
            direction: DIRECTION.FUTURE //getting users alphabetically
        }

        fetchUsers(dto);

        return () => {
            //abortController.abort();
        };

    }, [open, search, intersected, users, abortController, hasMore, requestFilter]);

    return (
        <div className="">
            <Button
                type="submit"
                variant="contained"
                onClick={() => { handleClickOpen() }}
                sx={{
                    position: "absolute",
                    bottom: 5,
                    right: 5,
                    width: '60px',
                    height: '60px',
                    borderRadius: '50%',
                }}
            >
                Go
            </Button>
            <Dialog
                open={open}
                onClose={() => handeClickClose()}
                fullWidth={true}
                maxWidth="sm"
            >
                <div className="participant-selection-popup-content-wrapper">
                    <FormControlLabel
                        control={<Switch
                            checked={!multiSelect}
                            onChange={(event: any) => setMultiSelect(!event.target.checked)}
                        />}
                        label="Private chat"

                    />
                    {
                        multiSelect &&
                        <TextField
                            label="Chat name"
                            required
                            onChange={(event: any) => setChatName(event.target.value)}
                        >

                        </TextField>
                    }
                    <div className="participant-selection-popup__SearchBar">
                        <SearchBar
                            onChange={setSearch}
                        />
                    </div>
                    <div className="participant-selection-popup__UserList">
                        <UserList
                            users={users}
                            checked={checked}
                            handleToggle={handleToggle}
                            ref={setLastElementRef}
                            multiSelect={multiSelect}
                        />
                    </div>
                    <div className="participant-selection-popup__Confirmation">
                        <Button
                            variant="contained"
                            onClick={() => { handeClickClose()}}
                            sx={{
                                marginRight: '8px'
                            }}
                        >
                            Cancel
                        </Button>
                        <Button
                            variant="contained"
                            disabled={checked.length === 0}
                            onClick={() => handleClickOk()}
                        >
                            Ready
                        </Button>
                    </div>
                </div>
            </Dialog>
        </div>
    )
};
export default UserSelection;